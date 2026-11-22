# Ekko – app Android

Assistente de voz que fica ouvindo em segundo plano (serviço em primeiro plano com microfone).
Diga "Ekko, que horas são?" mesmo com a tela bloqueada.

## Gerar o APK sem computador potente (GitHub)
1. Crie uma conta e um repositório novo em github.com.
2. Envie TODOS os arquivos desta pasta (inclusive a pasta oculta .github).
3. Abra a aba **Actions**, escolha "Gerar APK" e clique em **Run workflow**.
4. Ao terminar (uns 5 min), baixe o artefato **ekko-apk**, extraia e instale o app-debug.apk no celular
   (permita "instalar apps desconhecidos").

Para respostas com IA, crie em Settings > Secrets > Actions o segredo ANTHROPIC_KEY com sua chave da API Anthropic.
Sem a chave, perguntas gerais viram busca no Google. A chave fica dentro do APK: use só no seu aparelho.

## Alternativa: Android Studio
Abra a pasta, deixe sincronizar e clique em Run. Para IA, ponha ANTHROPIC_KEY=... em gradle.properties.

## Primeiro uso (tela do app)
1. Permitir microfone, contatos e ligações. 2. Permitir sobre outros apps. 3. Ignorar economia de bateria.
4. Ligar a Ekko. Uma notificação "Ekko está ouvindo" aparece.

## Comandos
hora, data, timer, alarme, lanterna, volume, pausar/continuar/próxima música, ligar para [contato],
abrir [app], tocar [música] (YouTube), rota para [lugar], pesquisar [assunto] e perguntas com IA.

## Limites
- Alguns celulares (Xiaomi, Samsung, Huawei) matam apps em segundo plano: trave o app nos recentes e libere bateria.
- O reconhecimento de voz do Google pode dar um "bip" ao reiniciar, e usa mais bateria.
- Não é possível ligar/desligar Wi-Fi ou Bluetooth nem ler SMS nas versões recentes do Android sem ser o assistente padrão do sistema.
