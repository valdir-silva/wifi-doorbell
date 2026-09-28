# Script para fazer o deploy das atualizações para o Servidor usando GIT
# Ele atualiza o repositório com git pull e reinicia o serviço Systemd.

$remoteHost = "servidor70"
$remotePath = "~/wifi-doorbell"
$serviceName = "wifi-doorbell-scanner.service"

Write-Host "🚀 Atualizando o servidor via Git Pull ($remoteHost)..." -ForegroundColor Cyan
ssh $remoteHost "cd $remotePath && git pull origin main && sudo systemctl restart $serviceName"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Servidor atualizado via Git e serviço reiniciado com sucesso!" -ForegroundColor Green
    Write-Host "📝 Para ver os logs em tempo real do servidor, você pode usar o comando:"
    Write-Host "   ssh $remoteHost `"sudo journalctl -u $serviceName -f`"" -ForegroundColor Yellow
} else {
    Write-Host "❌ Erro ao puxar as atualizações do Git ou reiniciar o serviço no servidor." -ForegroundColor Red
}

Write-Host "Pressione qualquer tecla para sair..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
