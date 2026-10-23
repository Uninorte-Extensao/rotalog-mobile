# Unificação visual e fluxos — versão 1.2 / código 3

## Escopo e decisões

Aplicados os cards do Perfil às telas de login, rota atribuída, preparação, recebimento, lista, detalhes e Histórico. `SectionCard` centraliza superfície, cantos de 20 dp, padding de 16 dp, borda `outline` a 40% e sombra de 2 dp. O Perfil conserva composição, Inter, paletas e preferências. O caminhão e vetores de localização, carga, calendário, telefone, confirmação e aviso ficam em `shared/AppIcons.kt`; sem novas dependências.

Rotas mostra a ação prioritária conforme os pré-requisitos e quatro etapas: Aceite → Checklist → Conferência → Iniciar. Checklist revogado volta a ser prioridade, preservando a carga recebida. A partida mantém sua confirmação separada. A ausência de rota oferece acesso aos cenários no Perfil, sem atribuir uma rota automaticamente. O recebimento mantém a ordem do manifesto, feedback de conferência e indicação explícita de nova conferência após resolução. A confirmação permanece no fim da lista, com bloqueios próximos.

A rota ativa usa um mapa estável, cabeçalho e resumo inferior de até metade da área disponível. Somente o resumo abrevia nome/endereço; a descrição acessível e os detalhes conservam os textos completos. Selecionar marcador atualiza somente a seleção. A lista abre os detalhes, inclusive antes do aceite.

O painel arrastável foi substituído deliberadamente por `Dialog` opaco de tela cheia. O mapa permanece composto com as mesmas dimensões e recuos; o contrato `RouteMap` recebe recuos superior/inferior independentes da abertura. A janela modal cobre a barra inferior, bloqueia toques no fundo e oculta a semântica do conteúdo coberto. Voltar fecha a consulta; o acionador recupera foco. Rolagem por parada e feedback ficam fora da composição interna do `Dialog`, sob `SaveableStateHolder`, permitindo reabertura e recriação. A chave antiga `expanded` migra para `detailsOpen`. Trocar rota/cenário fecha detalhes mesmo se os identificadores das paradas forem reutilizados.

Navegação externa continua em `ExternalActions`. O seletor fica acima dos detalhes e conserva falhas de Maps/Waze e feedback de cópia visíveis. Cópia direta e indisponibilidade do discador aparecem nos respectivos cards; o aviso entra na área visível e é anunciado. Retornar do aplicativo externo conserva a consulta.

Histórico ganhou destino por `routeId` dentro de seu próprio grafo, com resumo e consulta somente de leitura das paradas em ordem. Uma parada expandida por vez; expansão, destino e rolagem restauráveis. Registros ausentes recebem orientação de retorno. A consulta não usa seleção nem comandos da rota atual.

Grupos de métricas/ações empilham abaixo de 360 dp ou com fonte a partir de 150%, usando largura real da janela. Os conteúdos rolam e não limitam a escala do sistema. Login conserva largura máxima de 420 dp, campos Material, privacidade da senha, teclado e aviso de indisponibilidade.

## Referências e diferenças do Stitch

Projeto: https://stitch.withgoogle.com/projects/5674232400967196939. Referências e decisões detalhadas vieram do plano aprovado e do Perfil existente. Nesta sessão a ferramenta web não conseguiu acessar o projeto; não houve nova inspeção direta das imagens do Stitch. A revisão usa capturas reais do aplicativo.

Não foram introduzidos GPS, distâncias, ETA, capacidade de veículo, conectividade ou confirmações de servidor fictícias. Permanecem os dados demonstrativos e os totais derivados: oito paradas, oito produtos, 70 volumes para coleta e para entrega, sem somá-los como duas cargas. A camada modal, as etapas explícitas e a consulta do Histórico são adaptações funcionais aprovadas ao protótipo.

## Ciclos TDD

Testes executados antes da implementação de cada comportamento:

| Incremento | Red observado | Green / regressão direcionada |
| --- | --- | --- |
| Seleção independente | `selectionRestoresWithoutOpeningDetailsOrChangingOperation`: seleção abria `expanded` automaticamente | Seleção restaura sem abrir detalhes ou mudar operação |
| Camada modal | `detailsAreModalAndKeepMapGeometryOnReturn`: Voltar encerrava a Activity (`NoActivityResumedException`) | Modal restaura, Voltar fecha só a camada e geometria do marcador é preservada |
| Consulta do Histórico | `consultCompletedStopsAndRestoreExpansionWithoutChangingCurrentRoute`: ação `Ver paradas da rota` ausente | Consulta, expansão, recriação e isolamento da rota atual aprovados |
| Etapas e prioridade | `departureShowsFourStepsAndPrioritizesReceiptAfterPreparation`: `4. Iniciar` ausente | Quatro etapas e conferência prioritária após preparo aprovadas |
| Nova conferência | Regressão de divergência: `Aguardando nova conferência` ausente após resolução | Aviso e fluxo de nova conferência/recebimento/partida aprovados |
| Ações em andamento | `commandsKeepKnownContentAndPreventRepeatedActionsWhileBusy`: dois comandos executados em vez de um | Comando único, conteúdo preservado e liberação do estado ocupado aprovados |
| Migração da próxima parada | `legacyExpandedNextStopRestoresWhenNoMarkerWasSelected`: detalhes antigos não reabriam quando `expanded=true` e não havia marcador salvo | Migração resolve a próxima parada válida antes de restaurar a abertura; fechar e selecionar continuam independentes |
| Troca da rota | `replacingRouteClosesDetailsEvenWhenStopIdentifiersAreReused`: detalhes continuavam abertos | Trocar rota fecha a consulta sem reabri-la ao voltar ao cenário |
| Feedback externo | `externalFeedbackIsVisibleOnceInTheRelevantCard`: dois avisos do discador; após separar avisos, mensagem fora da área visível | Um aviso no card correto, trazido para a área visível |
| Rolagem ao reabrir | `mapViewportRemainsMountedWhileModalHidesBackgroundAndRestoresPerStopScroll`: rodapé não restaurado ao reabrir | Estado retirado do interior do Dialog; restauração por parada aprovada |
| Retorno de foco | Após recriar e fechar detalhes, `Ver detalhes` permanecia sem foco | Botão permite foco em modo de toque; retorno aguarda a janela recuperar foco após dispensar o Dialog. O teste aguarda o evento assíncrono e exige foco efetivo |
| Mapa com fonte 200% | Marcadores sob o resumo e alvos sobrepostos; seleção não atualizava o resumo | Cabeçalho compacto, resumo de até 40% e grade com alvos separados; oito marcadores entre legenda e resumo |
| Cenário vazio | `emptyRouteOffersProfileScenariosWithoutAssigningRoute`: acesso ao Perfil ausente | Acesso ao Perfil sem atribuição simulada |

Os testes do gesto do painel foram substituídos pelos testes do modal, preservando as asserções operacionais. Verificações complementares cobrem chave legada, identificador inválido, registro histórico ausente, expansão única, ausência de contato/produtos, textos longos e adaptação de ações. Essas verificações complementam os ciclos; não são apresentadas como red anterior à implementação. Refinamentos exclusivamente visuais são conferidos pelas capturas, sem testes de valores de borda/sombra.

Após green, mapa, resumo e camada foram separados em arquivos próprios. Removidas as dimensões e a geometria associadas ao painel expandido. As suítes afetadas foram repetidas na refatoração. Não foram alterados contratos de backend, modelos operacionais, versões ou dependências.

## Validação e artefatos

Ambiente: JDK 25 do Android Studio, AVD `Pixel_10a`, Android API 37, KVM instalado e utilizável (`emulator -accel-check`, código 0). Matriz principal em 720 × 1280 px a 320 dpi (360 × 640 dp), temas claro e escuro, fontes 100% e 200%. Teclado exercitado no login e no formulário de divergência, incluindo restauração das observações.

Comandos finais, sem filtros:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
# Depois de configurar a fonte em 200% no AVD:
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:connectedDebugAndroidTest
```

| Verificação final | Resultado |
| --- | --- |
| Build debug | Aprovado; versão 1.2 / código 3 |
| Unitários, todas as seções | 21 aprovados; zero falhas/erros/ignorados |
| Lint debug | Zero erros; 11 avisos de versões de dependências disponíveis |
| Instrumentados sem filtros, fonte 100% | 27 aprovados; zero falhas/erros/ignorados |
| Instrumentados sem filtros, fonte 200% | 27 aprovados; zero falhas/erros/ignorados |
| Adaptação em 359 dp | Ações e opções de tema em 100% e 200%; suíte dos detalhes em 150% aprovadas |
| Adaptação em 360 dp / 150% | Ações e opções de tema aprovadas |
| Revisão visual | Capturas claras/escuras em ambas as escalas; mapa, detalhes, login, Histórico, carregamento e Perfil inspecionados |
| Galeria | 418 PNGs completos de 720 × 1280 px: 154 em 100%, 264 em 200%; integridade do ZIP aprovada |
| Git | `git diff --check` aprovado; sem commit, push ou merge |

A regressão inicial em 100% encontrou o problema de foco descrito no ciclo TDD. Após a correção e a migração da próxima parada, a regressão completa foi repetida em ambas as escalas, com os resultados acima. Testes de limites posteriores usam filtros apenas para a checagem complementar de 359 dp; os relatórios completos foram preservados antes dessas execuções.

Artefatos locais, fora do Git:

- APK: [`app-debug.apk`](../app/build/outputs/apk/debug/app-debug.apk).
- [Galeria navegável](../app/build/review/ui-refinement/index.html), com filtros de área, tema e escala e imagens na resolução original.
- [Pacote ZIP da galeria](../app/build/review/ui-refinement/rotalog-1.2-galeria.zip).
- [Relatório completo em 100%](../app/build/review/ui-refinement/font-100/report/index.html) e [em 200%](../app/build/review/ui-refinement/font-200/report/index.html).
- Resultados XML completos em `app/build/review/ui-refinement/font-100/` e `font-200/`; resumo verificável em `validation-summary.json`.
- Logs finais em `app/build/review/ui-refinement/logs/` e capturas em `screenshots/`.

As sequências numeradas da galeria cobrem páginas inteiras com quadros sobrepostos, incluindo o último controle. Há aceite e partida, checklist, conferência completa, divergência, recebimento, mapa, camada aberta, produtos, lista, Histórico com parada expandida, Perfil recolhido/expandido e login com teclado, erros e indisponibilidade.

SHA-256 do APK: `5857e079a0f897fb057c13510e80738dc29e65c3b38eafcaf2b1dd8a63ac5dd1`.

O APK final foi instalado para revisão. Tamanho/densidade originais do AVD (1080 × 2424 px / 420 dpi) e fonte 100% foram restaurados após a matriz. Não foram executados testes em aparelho físico, em outras APIs Android, com TalkBack manual, trajetos/chamadas reais ou CI remota. Backend, autenticação e GPS continuam fora desta demonstração. A comparação direta com as imagens do Stitch permaneceu indisponível, conforme registrado acima.

As falhas red foram observadas durante a implementação. Os logs temporários anteriores à retomada não sobreviveram em `/tmp`; os registros preservados da validação final ficam em `app/build/review/ui-refinement/logs/`, fora do Git.

As configurações locais preexistentes em `.idea/deploymentTargetSelector.xml`, `.idea/gradle.xml` e `.idea/misc.xml` foram preservadas. Sem commit, push ou merge.
