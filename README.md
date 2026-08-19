# Punch Card Widget (Android)

Esqueleto funcional de um widget de tela inicial com "punch cards" (ícones de
livro clicáveis, tipo checklist de leitura), rolável e customizável.

## Como gerar o APK sem instalar nada no computador

O projeto já vem completo (Gradle + ícones) e com um workflow do
**GitHub Actions** em `.github/workflows/build.yml` que compila o APK na
nuvem. Você só precisa de um navegador:

1. **Crie uma conta no GitHub** (github.com) se ainda não tiver — é grátis.
2. Clique em **New repository** (canto superior direito → "+" → "New
   repository"). Dê um nome, ex: `punch-card-widget`, marque como
   **Public** (mais simples pro Actions rodar sem configuração extra) e crie.
3. Na página do repositório recém-criado, clique em **"uploading an existing
   file"** (ou Add file → Upload files).
4. Extraia o `.zip` que te enviei no seu computador (ou no próprio Google
   Drive/gerenciador de arquivos do navegador, sem precisar "instalar" nada —
   só descompactar) e arraste **a pasta `PunchCardWidget` inteira** para a
   área de upload do GitHub. Navegadores modernos (Chrome/Edge) aceitam
   arrastar pastas inteiras e o GitHub reconstrói a estrutura de subpastas
   automaticamente.
5. Clique em **Commit changes**.
6. Vá na aba **Actions** do repositório. O workflow "Build APK" deve começar
   a rodar sozinho (ele dispara a cada push). Se não iniciar automaticamente,
   clique em **Run workflow**.
7. Espere a build terminar (uns 3–6 minutos). Ao final, entre na execução
   concluída e baixe o arquivo em **Artifacts → punch-card-widget-debug-apk**
   — isso baixa um `.zip` contendo o `app-debug.apk`.
8. Abra esse link/arquivo **direto no celular** (pelo navegador do próprio
   Android, mandando o link por e-mail/Drive/WhatsApp pra você mesmo, ou
   baixando no PC só pra depois enviar pro celular). No Android, toque no
   `.apk` baixado → ele vai pedir permissão de "instalar apps desconhecidos"
   → autorize só pra esse arquivo → instala.
9. Depois de instalado, mantenha o dedo pressionado na tela inicial → Widgets
   → procure "Punch Card Widget" → arraste pra tela. Ele vai abrir a tela de
   configuração (customizar cada card) antes de finalizar.

Isso resolve o "não consigo baixar nada nesse PC": todo o trabalho pesado
(compilar) acontece nos servidores do GitHub, não no seu computador. O único
arquivo que você toca localmente é o APK final, e só se quiser — dá pra até
pular o PC e ir do link de download direto pro celular.

> Se algum passo da Action falhar (aparece um X vermelho na aba Actions),
> copie a mensagem de erro do log e me mostra — eu ajusto o workflow ou o
> código, porque nomes/versões de plugin do ecossistema Android mudam com
> frequência e não tenho como testar esse build aqui do meu lado.

## Como funciona (arquitetura)

Um widget de tela inicial **não é uma View normal** — ele roda via
`RemoteViews`, um conjunto limitado de views que o launcher consegue desenhar
em outro processo. Rolagem dentro de um widget só é possível com os
"Collection Widgets": `ListView`, `GridView` ou `StackView`, alimentados por
um `RemoteViewsService`. É esse o mecanismo usado aqui:

- **`PunchCardWidgetProvider`** — cria o widget, aponta o `GridView` para o
  serviço de dados e define um `PendingIntentTemplate` único para todos os
  cliques da lista.
- **`PunchCardRemoteViewsService` / `PunchCardViewFactory`** — gera a `RemoteViews`
  de cada card individualmente (ícone + texto opcional) e anexa um
  `fillInIntent` dizendo *qual* card foi tocado.
- **`PunchCardStore`** — guarda os cards de cada instância do widget.
  Cards com `persist = true` vão para `SharedPreferences` (sobrevivem a
  reinício do aparelho). Cards com `persist = false` ficam só em um cache em
  memória (some se o processo do widget for encerrado).
- **`PunchCardConfigActivity`** — tela mostrada quando o usuário arrasta o
  widget pra tela inicial: escolhe quantos cards, texto de cada um (ou
  nenhum) e se aquele card deve salvar o estado.

## Customização por card

Cada `PunchCardItem` tem:

```kotlin
data class PunchCardItem(
    val id: Int,
    var iconRes: String,        // drawable quando não lido
    var iconResChecked: String, // drawable quando lido
    var label: String?,         // null = sem texto
    var checked: Boolean,
    var persist: Boolean        // false = não salva, só memória
)
```

Pra ir além (ícone customizado por imagem escolhida da galeria, cores, etc.)
dá pra estender esse modelo — o gargalo é sempre "isso pode ser desenhado com
`RemoteViews`?" (ver limitações abaixo).

## Limitações reais de widgets Android (importante saber)

- **Não dá pra usar qualquer View.** `RemoteViews` só suporta um subconjunto
  fixo (TextView, ImageView, Button, ProgressBar, ListView/GridView/StackView,
  ViewFlipper, entre poucos outros). Nada de `RecyclerView`, `Compose` direto,
  Views customizadas, etc.
- **Sem animações elaboradas** — só as poucas que o `RemoteViews` expõe.
- **Cliques em itens de coleção** exigem sempre o padrão
  `setPendingIntentTemplate` + `fillInIntent` (não dá pra usar
  `PendingIntent` direto por item, por limite do sistema).
- **Atualização de dados** exige chamar
  `notifyAppWidgetViewDataChanged` — o widget não observa o
  `SharedPreferences` sozinho.
- Ícone customizado por imagem (ex: capa de livro) funciona bem
  (`setImageViewBitmap`/`setImageViewUri`), mas texto tem limites de
  formatação (sem HTML rico).

Se em algum momento voc월 quiser algo visualmente mais livre (gradientes,
fontes customizadas, layouts totalmente livres), a alternativa seria abrir
uma Activity/BottomSheet ao tocar no widget — mas aí a rolagem "dentro" do
próprio widget deixa de ser necessária, porque a tela cheia resolve.

## Próximos passos sugeridos

1. Trocar o ícone fixo por seletor de imagem (ex: capa do livro via
   Storage Access Framework) na tela de configuração.
2. Permitir editar um widget já existente (reabrir `PunchCardConfigActivity`
   com os valores atuais pré-carregados).
3. Adicionar suporte a "long press" pra editar um card individual sem
   precisar reconfigurar o widget inteiro.
