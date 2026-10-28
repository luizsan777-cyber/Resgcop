# RegsCo

Aplicativo Android local de compras domésticas (`com.regsco.compras`). Esta evolução preserva o banco `regsco.db` do projeto inicial e **ainda não representa a conclusão integral do prompt mestre**.

## Implementado nesta evolução

- Compras, edição de dados, duplicação como compra aberta, finalização/reabertura e orçamento por mês/ano.
- Valores em centavos, descontos por item e regra de atacado conforme quantidade mínima, com confirmação do total.
- Marca/apresentação, unidade, código de barras, observações e estabelecimento por item.
- Dashboard com gasto efetivo, projeção, alertas e navegação por módulos.
- Lista pesquisável por categoria e situação, compartilhamento, importação para compras e cesta recorrente.
- Conversão kg↔g e L↔mL no cálculo da reposição; unidades incompatíveis não são somadas.
- Estoque editável, mínimos, validade e entrada confirmada de compra finalizada com proteção contra repetição.
- Histórico com filtros e paginação; comparação local separada por marca/apresentação e unidade.
- Área de farmácia baseada na categoria Farmácia, com apresentação/dosagem informadas pelo usuário e gastos no orçamento geral.
- Relatórios por categoria/loja, comparação mensal, seis meses, resumo compartilhável, CSV e PDF.
- CameraX com foco, lanterna, captura em armazenamento privado e recorte manual. OCR e leitura EAN/UPC por modelos ML Kit incluídos no APK. Reconhecimento não salva preços automaticamente.
- Consulta de código no histórico local. Sem consulta automática externa.
- Backup JSON v4 sequencial; leitura dos formatos v1–v3, validação em banco temporário e substituição transacional após confirmação. Sem limite artificial de tamanho de arquivo.
- Temas Verde Natureza, Rosa e Nebulosa; índices SQLite; testes de integridade, migração e telas.

## Limitações conhecidas — não ocultar ao entregar

- Quantidades digitadas ainda são inteiras por unidade; compras fracionárias por peso precisam de implementação e testes adicionais.
- OCR apresenta candidatos de preço para escolha; não classifica com confiabilidade varejo/atacado/nome/quantidade mínima automaticamente.
- Não há integrações de preços online nem consulta externa de códigos de barras. Nenhum preço é simulado.
- A farmácia não tem cadastro independente com orçamento próprio; é uma área das compras domésticas categorizadas.
- Algumas listas secundárias ainda não são paginadas. Relatórios CSV e compartilhamento de listas montam o texto em memória.
- A entrada de estoque é feita para todos os itens da compra após confirmação. Alterar ou reabrir a compra não desfaz movimentos físicos já confirmados; ajustar estoque manualmente.
- A câmera acompanha a paleta escolhida. Os três temas usam fundos nativos com folhas/estrelas, cartões, indicador circular e navegação inferior; não são uma reprodução pixel a pixel das imagens de referência. Revisão visual em aparelho físico e animações completas ainda pendentes.
- Formulários não salvos podem ser perdidos se o Android encerrar o processo enquanto a câmera está aberta.
- Validação em Redmi 13 físico, OCR em etiquetas reais, foco/flash e fluxo de compartilhamento ainda necessários.
- APK de desenvolvimento usa chave debug. Uma instalação anterior assinada com outra chave não poderá ser atualizada diretamente. **Não desinstalar sem exportar e guardar um backup.**

## Arquitetura e dados

A interface Java nativa existente foi evoluída, sem substituir o aplicativo. `MainActivity` mantém as telas existentes e a navegação. `DatabaseRules` aplica adições de colunas e índices sem apagar registros; `BackupIO` centraliza transferência e validação; `Money`, `Units` e `LabelParser` isolam regras testáveis; `ScanActivity` contém câmera/recorte/reconhecimento local.

Banco e preferências continuam locais. O aplicativo não declara permissão de Internet. A câmera é solicitada somente ao abrir a leitura. Fotos são temporárias, no cache privado. Não há anúncios, conta, telemetria ou assinatura.

Backup v4 é JSON UTF-8 com `format=regsco-backup`, `version=4` e arrays das tabelas `settings`, `sessions`, `items`, `budgets`, `shopping_list`, `pantry`, `basket`, `stores`. Colunas adicionadas recebem padrões ao ler backups antigos. O backup não é criptografado; guardar o arquivo em local privado.

## Compilação e testes

JDK 17 completo, Android SDK 35 / Build Tools 35.0.0 e Gradle 8.11.1:

```sh
gradle --no-daemon clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

O workflow `.github/workflows/android.yml` compila, testa e executa lint em `main` e `codex/**`, publicando APK e relatórios. Testes Robolectric de integridade usam API 28; testes de telas usam API 35. Esses testes não substituem validação em hardware real.

## Fontes técnicas e pesquisa de integrações

- https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- https://developer.android.com/media/camera/camerax/preview
- https://developer.android.com/jetpack/androidx/releases/exifinterface
- https://ofertasmateus.com/ — portal de ofertas localizado na pesquisa; não integrado nem validado como API pública.
- https://play.google.com/store/apps/details?id=br.com.carvalhosupershop.appvendas — aplicativo oficial localizado; não fornece nesta pesquisa contrato de API pública para o RegsCo.

Pesquisa em 27/10/2026. Nenhuma afirmação de inexistência universal de APIs: apenas não foi confirmada uma integração pública adequada nesta implementação.

## Referências visuais aplicadas em 0.3.0

Verde Natureza (bege e folhagens), Roxo Nebulosa (roxo profundo com estrelas e rosa) e Rosa Claro (rosa/lavanda). Recursos decorativos são desenhados nativamente em `ThemeSkin`, sem rede. Todos os valores e produtos exibidos vêm do banco local, não dos exemplos das imagens.
