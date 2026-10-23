# RotaLog Entregador

Aplicativo Android nativo para o motorista do marketplace B2B RotaLog. A versão 1.2 contém uma demonstração operacional com Kotlin, Jetpack Compose e Material 3.

## O que está implementado

- **Login:** tela inicial baseada nos protótipos claro e escuro do Stitch, validação local de e-mail/senha, mostrar/ocultar senha e orientação de recuperação. Autenticação ainda não conectada; **Explorar demonstração** abre os dados fictícios sem usar credenciais.
- **Rotas:** rota atribuída, confirmação explícita de aceite, checklist com quatro itens e conferência física por produto.
- **Recebimento no CD:** manifesto de oito produtos / 70 volumes, divergências locais, resolução explícita e confirmação de recebimento. O início é uma ação separada, liberada somente após checklist e recebimento.
- **Rota ativa demonstrativa:** oito paradas na ordem da operação, mapa ilustrativo nativo estável, progresso, lista e resumo inferior. Os detalhes completos abrem em uma camada opaca de tela cheia, mantendo o mapa montado e com as mesmas dimensões.
- **Ações externas:** escolha de Google Maps, Waze ou copiar endereço; chamada pelo discador. Aplicativos ausentes recebem mensagem e alternativa de copiar o endereço.
- **Histórico:** uma rota anterior fictícia concluída, com consulta das paradas, endereço, janela, instruções e produtos. Uma parada expandida por vez, com restauração da consulta e sem interferir na rota atual.
- **Perfil:** identidade demonstrativa e veículo em cards próprios, opções visuais de tema do aparelho/claro/escuro e seção **Ambiente de demonstração**, inicialmente recolhida, com resumo do cenário e envio simulado. Abra a seção para acessar os três cenários, reinício e seis estados de envio. Expansão e rolagem são restauradas ao recriar a Activity e retornar à aba; os temas ficam empilhados abaixo de 360 dp ou com fonte a partir de 150%.

Ao abrir, o aplicativo apresenta **Entrar na sua conta**. O botão **Entrar** valida os campos e informa que a autenticação está indisponível; nenhum dado é enviado e nenhuma sessão autenticada é criada. **Esqueci minha senha** orienta procurar a operação, sem simular envio de e-mail. Use **Explorar demonstração** para acessar o cenário padrão **Rota atribuída**. Aceitar a rota apenas confirma o aceite: não recebe mercadorias, prepara o veículo, inicia rota ou coleta localização. No Perfil, **Rota ativa demonstrativa** carrega um exemplo independente, com duas de oito paradas concluídas. Selecionar uma parada nunca altera seu estado operacional.

Para percorrer o fluxo inspirado no Stitch: **Aceitar rota → Preparar saída → marcar os quatro itens → Confirmar checklist → Avançar para conferência → conferir os oito produtos → Confirmar recebimento da carga → Voltar à rota para iniciar → Iniciar rota**. As três confirmações (aceite, recebimento e partida) são explícitas.

Uma divergência desmarca o produto e bloqueia o recebimento. Após resolver com a equipe do CD, registre a resolução e confira novamente o produto. Notas ficam limitadas a 500 caracteres e não são enviadas ao CD; não há foto, assinatura ou notificação real. A conferência fica disponível para consulta após o recebimento. Revisar e desmarcar um item do checklist revoga o preparo, mas preserva o registro da carga recebida; a partida volta a ser bloqueada até reconfirmar o checklist.

Receber conclui somente a parada de coleta. Ao iniciar a rota atribuída, a próxima parada é a **Drogaria Rio Negro**, com zero volumes entregues. O exemplo ativo do Perfil continua independente. Tocar no cenário já selecionado preserva o progresso; use **Reiniciar cenário atual** para zerá-lo.

A carga total é calculada a partir dos itens: **70 volumes previstos para coleta e 70 para entrega**, sem somar coleta e entrega como cargas diferentes. No cenário ativo do Perfil, 70 foram coletados e oito entregues. O Supermercado Amazonas tem 14 volumes: oito caixas de óleo, quatro fardos de arroz e duas caixas de café.

A tela Rotas destaca a próxima ação conforme o aceite, checklist e recebimento e mostra as quatro etapas antes da saída. Após preparar, a ação principal passa a ser conferir a carga; após receber, iniciar a rota. Desmarcar o checklist volta a priorizar sua revisão.

No mapa, tocar em um marcador altera somente o resumo. **Ver detalhes** abre a consulta completa, que cobre também a navegação inferior. Voltar fecha a camada e preserva seleção, enquadramento do mapa e rolagem por parada. Na lista, tocar em uma parada abre seus detalhes, inclusive antes do aceite.

O mapa exibe **“Mapa demonstrativo • sem GPS”**. Os caminhos são vetoriais ilustrativos, sem cálculo de trajetos pelas ruas. Nenhuma permissão de GPS, câmera ou chamada direta é solicitada.

## Executar

Requisitos: JDK 25, Android SDK `platforms;android-37.0`, Build Tools 36.0.0 e dispositivo/emulador API 29+. Configure `sdk.dir` no `local.properties` (fora do Git) ou `ANDROID_HOME`.

```bash
export JAVA_HOME=/opt/android-studio/jbr # adapte ao JDK 25 local
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

No Android Studio, abra a raiz, aguarde o Gradle Sync e execute `app`. No Windows, use `gradlew.bat`.

APK: [`app/build/outputs/apk/debug/app-debug.apk`](app/build/outputs/apk/debug/app-debug.apk).

Revisão desta unificação visual: [galeria de capturas](app/build/review/ui-refinement/index.html) e [pacote ZIP](app/build/review/ui-refinement/rotalog-1.2-galeria.zip), gerados localmente e fora do Git. Os resultados e ciclos TDD estão em [docs/UI-REFINEMENT-VALIDATION.md](docs/UI-REFINEMENT-VALIDATION.md).

## Verificar

Durante o desenvolvimento, execute somente os testes das seções alteradas e fluxos relacionados. Consulte os pacotes e comandos em [docs/TESTING.md](docs/TESTING.md) (Login, Rotas, Preparação/recebimento, Histórico e Perfil). Ao finalizar, execute a regressão completa abaixo, **sem filtros**, para detectar efeitos colaterais.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Com dispositivo/emulador conectado:
./gradlew :app:connectedDebugAndroidTest
```

Os testes cobrem aceite repetido, bloqueios de preparação/recebimento/partida, conferência integral, divergências e rechecagem, restauração do checklist e formulário, isolamento de cenários, totais, próxima parada, seleção, recriação, temas, expansão/recolhimento e restauração do Perfil, seleção acessível e adaptação das opções de tema, envio simulado e contratos de intents com aplicações ausentes. A CI executa build, testes unitários e lint na raiz; publica APK e relatórios como artifacts. Os testes instrumentados são executados localmente no AVD com KVM.

Consulte a unificação visual e a consulta do Histórico em [docs/UI-REFINEMENT-VALIDATION.md](docs/UI-REFINEMENT-VALIDATION.md), a validação do Perfil em [docs/PROFILE-VALIDATION.md](docs/PROFILE-VALIDATION.md), a validação do login em [docs/LOGIN-VALIDATION.md](docs/LOGIN-VALIDATION.md), o registro operacional em [docs/LOADING-VALIDATION.md](docs/LOADING-VALIDATION.md) e o histórico da primeira entrega em [docs/VALIDATION.md](docs/VALIDATION.md).

## Arquitetura

Um módulo `:app`, composição manual de dependências, fluxo unidirecional e estado imutável:

```text
com.rotalog.delivery/
  AppContainer.kt        # dependências com escopo da aplicação
  MainActivity.kt        # bootstrap e Navigation Compose
  model/                 # rotas, paradas, itens e estados independentes
  data/                  # contratos, fixtures e DataStore
  routes/                # ViewModel + StateFlow, telas e mapa Canvas
  login/                 # formulário local + ViewModel/StateFlow, sem autenticação
  history/               # histórico demonstrativo
  profile/               # preferências e controles da demonstração
  shared/                # componentes, vetores e intents Android
  ui/theme/              # paletas fixas, Inter, formas e espaçamento
```

O `RoutesViewModel` pertence ao grafo principal e é compartilhado entre Rotas, lista de paradas e controles do Perfil. A navegação transporta apenas `routeId`. `SavedStateHandle` restaura cenário, seleção, abertura dos detalhes (com migração da antiga chave `expanded`), estado da rota, checklist, conferência, divergências e envio em recriação; a navegação e a rolagem usam a restauração do Compose. O repositório demonstrativo permanece na memória da aplicação. **Somente a preferência de tema tem persistência durável**, em DataStore; encerrar o processo retorna a operação à demonstração padrão. Não existe fila offline real. No login, e-mail e escolha de explorar a demonstração são restaurados com a tarefa via `SavedStateHandle`. A senha e sua visibilidade ficam somente no ViewModel: sobrevivem à rotação, mas não são gravadas no estado salvo e são apagadas ao explorar a demonstração.

Os seis estados de envio (salvo no aparelho, aguardando, enviando, confirmado, falha e conflito) são escolhidos manualmente no Perfil e identificados como simulados. Não há confirmação automática por conectividade ou temporizador. “Confirmado” não muda a situação operacional da rota.

Versões de referência preservadas: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Compose BOM 2026.02.01, `compileSdk/targetSdk 37`, `minSdk 29`. Dependências adicionais: Navigation Compose 2.9.5, Lifecycle Compose 2.9.4 e DataStore Preferences 1.1.7. AndroidX Test JUnit 1.3.0 e Espresso 3.7.0 substituem os testes antigos incompatíveis com o Android 37 (`InputManager.getInstance`). Nenhuma atualização da stack principal foi necessária. Fonte Inter e licença: [docs/licenses](docs/licenses/README.md).

## Limites e próxima entrega

A tela de login está implementada, mas não há backend, autenticação real, evidências digitais, ocorrências de entrega, devoluções, encerramento, mapas reais ou rastreamento. Checklist, conferência, divergências no CD e partida funcionam somente na demonstração. O histórico não fornece comprovantes reais. Telefone e identidade são fictícios.

A próxima etapa é **execução da entrega na parada** (início da tentativa e registro do resultado), seguida da integração com o contrato canônico OpenAPI do backend. Não são necessárias credenciais externas nesta demonstração.

Para substituir o mapa demonstrativo por Google Maps SDK/Maps Compose, serão necessários projeto Google Cloud, faturamento, SDK habilitado e chave restrita ao aplicativo fora do Git. O SDK desenhará ruas; geometria de percurso deverá vir da operação ou de serviço de rotas.

## Referências

- [Design no Stitch](https://stitch.withgoogle.com/projects/5674232400967196939)
- [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Canvas no Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [AndroidX Test: versões e notas de compatibilidade](https://developer.android.com/jetpack/androidx/releases/test)
- [Maps Compose](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
- [Backend](https://github.com/Uninorte-Extensao/rotalog-backend) · [Portal](https://github.com/Uninorte-Extensao/rotalog-frontend)
