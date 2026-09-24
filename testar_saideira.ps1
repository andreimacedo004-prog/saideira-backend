# ---------------------------------------------------------------
# Passeio completo pela API do Saideira.
# Suba o backend (mvn spring-boot:run) e rode num outro terminal:
#   powershell -ExecutionPolicy Bypass -File .\testar_saideira.ps1
# Cada execucao cria usuarios novos, entao pode rodar quantas vezes quiser.
# ---------------------------------------------------------------
$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8280"
$sufixo = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

function Chamar($metodo, $rota, $token, $corpo) {
    $params = @{ Uri = "$baseUrl$rota"; Method = $metodo; ContentType = "application/json; charset=utf-8" }
    if ($token) { $params.Headers = @{ Authorization = "Bearer $token" } }
    if ($corpo) { $params.Body = [System.Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Depth 5)) }
    # Guardar antes de devolver faz listas JSON virarem itens no pipeline
    # (sem isso, o ForEach-Object recebe a lista inteira como um item so)
    $resposta = Invoke-RestMethod @params
    return $resposta
}

function MensagemDeErro($erro) {
    try { return ($erro.ErrorDetails.Message | ConvertFrom-Json).erro } catch { return $erro.ErrorDetails.Message }
}

Write-Host "=== 1) regras do jogo (publico) ==="
$regras = Chamar Get "/api/regras"
Write-Host "check-in +$($regras.pontosPorCheckIn) | cerveja nova +$($regras.pontosPorCervejaNova) | amigo +$($regras.pontosPorAmigoMarcado) | lugar novo +$($regras.pontosPorLugarNovo) | intervalo $($regras.intervaloMinimoMinutos) min"

Write-Host "`n=== 2) cadastro da Ana e do Beto ==="
$ana = Chamar Post "/api/auth/cadastro" $null @{ email = "ana.$sufixo@saideira.dev"; senha = "senha12345"; nome = "Ana"; maiorDeIdade = $true }
$beto = Chamar Post "/api/auth/cadastro" $null @{ email = "beto.$sufixo@saideira.dev"; senha = "senha12345"; nome = "Beto"; maiorDeIdade = $true }
$tokenAna = $ana.token
$tokenBeto = $beto.token
Write-Host "Ana id=$($ana.usuario.id) | Beto id=$($beto.usuario.id)"

Write-Host "`n=== 3) Ana cria o grupo e o Beto entra pelo convite ==="
$grupo = Chamar Post "/api/grupos" $tokenAna @{ nome = "Resenha $sufixo" }
$convite = Chamar Get "/api/grupos/$($grupo.id)/convite" $tokenAna
Write-Host "link de convite: $($convite.link)"
$grupo = Chamar Post "/api/grupos/entrar" $tokenBeto @{ codigoConvite = $convite.codigo }
Write-Host "membros: $(($grupo.membros | ForEach-Object { $_.nome }) -join ', ')"

Write-Host "`n=== 4) desafio comecando hoje, por 60 dias ==="
$desafio = Chamar Post "/api/grupos/$($grupo.id)/desafios" $tokenAna @{
    nome       = "Roles de Fim de Ano"
    dataInicio = (Get-Date).ToString("yyyy-MM-dd")
    dataFim    = (Get-Date).AddDays(60).ToString("yyyy-MM-dd")
}
Write-Host "desafio '$($desafio.nome)' ($($desafio.dataInicio) a $($desafio.dataFim)) - $($desafio.status)"

Write-Host "`n=== 5) busca no catalogo de cervejas ==="
$cervejas = @(Chamar Get "/api/cervejas?busca=heineken" $tokenAna)
$heineken = $cervejas[0]
Write-Host "achei: $($heineken.nome) (id $($heineken.id))"

Write-Host "`n=== 6) check-in da Ana no bar, marcando o Beto (esperado: 10 + 5 + 3 + 5 = 23) ==="
$checkIn = Chamar Post "/api/desafios/$($desafio.id)/checkins" $tokenAna @{
    tipo       = "BAR"
    local      = "Bar do Zé"
    legenda    = "Só mais uma"
    amigosIds  = @($beto.usuario.id)
    cervejaIds = @($heineken.id)
}
Write-Host "pontos do check-in: $($checkIn.pontos.total)"

Write-Host "`n=== 7) segundo check-in em seguida (esperado: recusado pelo intervalo de 2h) ==="
try {
    Chamar Post "/api/desafios/$($desafio.id)/checkins" $tokenAna @{ tipo = "BAR"; local = "Outro bar" } | Out-Null
    Write-Host "ATENCAO: deveria ter sido recusado"
} catch {
    Write-Host "recusado como esperado: $(MensagemDeErro $_)"
}

Write-Host "`n=== 8) Beto reage e comenta ==="
$reacoes = @(Chamar Put "/api/checkins/$($checkIn.id)/reacoes/BRINDE" $tokenBeto)
Write-Host "reacoes: $(($reacoes | ForEach-Object { "$($_.tipo) x$($_.total)" }) -join ', ')"
$comentario = Chamar Post "/api/checkins/$($checkIn.id)/comentarios" $tokenBeto @{ texto = "Lenda!" }
Write-Host "comentario: $($comentario.autor.nome): $($comentario.texto)"

Write-Host "`n=== 9) feed do desafio ==="
Chamar Get "/api/desafios/$($desafio.id)/checkins" $tokenBeto | ForEach-Object {
    Write-Host "$($_.autor.nome) @ $($_.local) [$($_.tipo)] +$($_.pontos.total) pts | $($_.totalComentarios) comentario(s)"
}

Write-Host "`n=== 10) ranking ==="
Chamar Get "/api/desafios/$($desafio.id)/ranking" $tokenBeto | ForEach-Object {
    Write-Host "$($_.posicao). $($_.usuario.nome) - $($_.pontos) pts ($($_.checkIns) check-in(s))"
}
