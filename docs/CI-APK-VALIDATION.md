# Distribuição automática do APK

Validação em 23/10/2026. Aplicativo 1.2 (`versionCode` 3), baseado no commit
`760d9ff`, com alterações locais no workflow e documentação.

## Comportamento

O workflow existente já atendia push na `main`, pull requests e execução manual.
A mudança separa o APK dos relatórios, exige sucesso nas verificações antes do
upload do APK e falha se o arquivo esperado não existir. O nome do artifact
identifica execução e commit; a retenção solicitada é de 30 dias. Relatórios
continuam sendo enviados em caso de falha. O README explica como baixar e instalar.

O APK usa a assinatura debug gerada pelo Android Gradle Plugin; não requer secrets.
Não há publicação na Play Store nem assinatura de produção. Chaves de runners
diferentes podem exigir desinstalar a versão anterior, conforme descrito no README.

## TDD e verificações

- Red: adicionados os contratos em `tools/ci/test_workflow.py` antes da alteração
  do workflow. A execução encontrou três testes: o gatilho da `main` passou;
  dois falharam com `0 != 1`, pela ausência dos artifacts separados de APK e
  relatórios. Não houve falha de ambiente ou compilação nessa etapa.
- Green: após separar os uploads e configurar identificação, retenção e erro
  para APK ausente, os três testes passaram.
- Refactor: extraída a seleção de uploads por caminho para um helper de teste;
  a suíte de contratos foi executada novamente e permaneceu verde.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`:
  `BUILD SUCCESSFUL`. Build e resultados unitários foram reutilizados pelo Gradle.
- Para confirmar execução efetiva dos unitários, executado
  `./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest`:
  `BUILD SUCCESSFUL`, 31 testes, zero falhas, erros ou testes ignorados nos XMLs.
- `git diff --check`: aprovado.

PyYAML é usado somente na validação do contrato YAML, em ambiente virtual isolado;
não adiciona dependência ao aplicativo. O workflow também executa esses contratos.

## Limitações

Na validação inicial, a execução remota e o download real pelo GitHub Actions
não haviam sido realizados. O teste de contrato verifica a configuração
declarada; não simula os servidores do GitHub.

Não executados testes instrumentados ou revisão visual em emulador: a mudança
é restrita à CI e não altera interface, fluxo do aplicativo ou suítes Android.
As alterações preexistentes em `.idea/` foram preservadas.

## Correção da instalação do SDK

O job `120692827904` da execução `40035198023` falhou ao executar
`sdkmanager tools`: `Warning: Failed to find package 'tools'`. A v3 de
`android-actions/setup-android` usa `tools platform-tools` por padrão. O pacote
`tools` foi removido do repositório do Android. Definido `packages: platform-tools`
explicitamente; os pacotes do projeto continuam na etapa seguinte.

Regressão: antes da correção, o novo teste falhou com `'tools' unexpectedly found
in ['tools', 'platform-tools']`; após a configuração explícita, os quatro testes
de contrato passaram. Não foi necessária refatoração adicional.

### Validação na main

A correção foi enviada diretamente para `main` no commit `76317c0`, junto da
distribuição do APK antes disponível em `development`.

- Validação local completa no checkout da main:
  `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, com
  `ANDROID_HOME` apontando para o SDK local: `BUILD SUCCESSFUL`, 51 tarefas
  executadas. Os quatro testes do workflow também passaram.
- [Execução 40035926558](https://github.com/ivanmatheus-dev/rotalog-mobile/actions/runs/40035926558):
  todas as etapas aprovadas, incluindo configuração/instalação do SDK,
  contratos, build, testes unitários, lint e uploads.
- Artifact `rotalog-debug-4-76317c0b9f11b5f7e4032cce672e0a40103e6865`
  (`12537755189`): download efetivamente realizado, 12.188.272 bytes.
  ZIP íntegro, contendo `app-debug.apk`; o APK contém manifesto e classes DEX.
  SHA-256 do ZIP conferido com o digest publicado pelo GitHub:
  `86cfd8ad709b5b991b41274b909acb15485f4ca95ea060dd0d6ecbf618c9fa87`.
