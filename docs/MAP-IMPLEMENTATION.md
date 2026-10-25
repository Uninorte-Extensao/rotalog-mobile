# Mapa de rotas — versão 1.0

## Escopo

Mapa real (MapLibre + OpenFreeMap) no fluxo de saída e na rota ativa, no lugar do mapa demonstrativo em Canvas. Inclui rota pelas ruas, posição do entregador pelo GPS e coordenadas nas paradas demonstrativas. A branch `adicao-mapa` foi integrada à `main` antes da abertura do PR para `development`; o merge está registrado ao final.

Foram adicionadas as dependências `maplibre-compose` e seu runtime OpenGL (0.20.0). O GPS do aparelho passou a ser usado apenas para exibir a posição do entregador no mapa. Não foram adicionadas autenticação, notificações ao CD ou confirmações reais de servidor.

## Referências e comportamento

O mapa implementa o contrato `RouteMap` existente (`topInset` e `bottomInset`), sem alterar a assinatura. `MapLibreRouteMap` é o padrão de `RoutesScreen`; `DemonstrationRouteMap` permanece no código como alternativa.

Na etapa Aceite do `DepartureFlow`, o componente `MapCourse` mostra o percurso completo no topo da tela. Na rota ativa, o mesmo `routeMap` é usado atrás da bottom sheet, com a câmera respeitando os insets da sheet.

Camadas, de baixo para cima: linha da rota, anel da parada selecionada (laranja), paradas pendentes (cinza), parada concluída (azul) e a posição do entregador (roxo). A câmera enquadra a rota inteira ao abrir e aproxima da parada ao selecionar outra.

A linha do percurso é calculada no OSRM, passando por todas as paradas na ordem, e serve como prévia visual. Enquanto carrega, o mapa não desenha linha; sem rede, usa a linha reta entre as paradas e registra a falha no Logcat. A navegação passo a passo é feita fora do app: ao iniciar a entrega, o entregador escolhe entre Waze, Google Maps ou copiar o endereço.

A posição vem do `LocationManager`, com atualização a cada 2 s ou 5 m. Com permissão precisa usa o GPS; só com a aproximada, usa a rede. O pedido de permissão é feito uma vez por instalação da tela; se negado, o mapa funciona sem o ponto de posição.


Diferenças deliberadas: a câmera não segue o entregador; não há botão de centralizar, selo de fluxo nem números destacados; o toque no marcador não seleciona a parada (a seleção continua pela lista de paradas).

## Requisições externas

Os blocos do mapa vêm de `tiles.openfreemap.org`, e as coordenadas das paradas são enviadas ao servidor público de demonstração do OSRM (`router.project-osrm.org`), sem garantia de disponibilidade. A posição do GPS não é enviada a nenhum serviço. O manifesto passou a declarar `INTERNET`, `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION`.

## Toolchain

O `maplibre-compose` 0.20.0 traz `kotlin-stdlib` 2.4.20, e o compilador padrão do AGP 9 (Kotlin 2.2) não lê esses metadados ("compiled with an incompatible version of Kotlin"). O Kotlin Gradle Plugin e o plugin do Compose passaram para 2.4.0 em `build.gradle.kts` e `libs.versions.toml`.

## TDD e regressão

Mapa, GPS e rota pelas ruas foram validados manualmente pelo emulador e por um smartphone físico; não há testes automatizados para essa parte, e não se apresenta nenhum ciclo red/green para ela. Problemas encontrados na validação manual: incompatibilidade de metadados do Kotlin (corrigida na toolchain) e queda ao abrir o app por criação do `AppContainer` fora de `lazy`.

| Verificação | Resultado real     |
|---|--------------------|
| Unitários | 31 de 31           |
| Lint debug | 0 erros, 13 avisos |
| Instrumentados | Não Executados     |
| SHA-256 do APK | f22a61a0968f7788b355c1211cd49bdcaacf67e708a1cc36f8dc5766065867b3            |


## Integração

A `main` foi incorporada à `adicao-mapa`. Os conflitos em `LoadingScreens.kt` e `RouteScreens.kt` foram resolvidos adotando o `DepartureFlow` da `main`, com o parâmetro `routeMap` reaplicado no fluxo novo. O mapa colocado antes nas telas `AssignedRoute` e `PreparationScreen` não foi mantido, porque a `main` removeu essas telas.

## Próximos passos
 - Implementar navegação offline
 - Incluir destaques na ordem de cada rota
 - Centralizar mapa e destacar posição do motorista
 - Diferenciar as rotas entre uma parada e outra