# TesseraUI

A lightweight HTML + CSS template renderer for NeoForge mods.  
Write your UI in plain HTML and CSS — TesseraUI parses it at runtime and renders native Minecraft widgets.  
No JavaScript, no browser engine.

**Version 1.1** — NeoForge 1.21.1 · Java 21

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21

## Installation

Add TesseraUI as a dependency in your `build.gradle`:

```groovy
repositories {
    maven { url "https://cursemaven.com" }
}

dependencies {
    implementation fg.deobf("curse.maven:tesseraui-<project-id>:latest")
}
```

Declare it in your `neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
    modId = "tesseraui"
    type = "required"
    versionRange = "[1.0,)"
    ordering = "NONE"
    side = "CLIENT"
```

## Quick start

**`assets/yourmod/ui/my_screen.html`**
```html
<col>
  <h2>My Mod</h2>
  <p>Welcome, {{ player.name }}!</p>
  <button onclick="openSettings">Settings</button>
</col>
```

**`assets/yourmod/ui/my_screen.css`**
```css
col {
  background: #1A1208;
  padding: 12px;
  gap: 6px;
}

h2    { color: #F0B27A; font-size: 10px; }
p     { color: #F3E7D3; font-size: 7px; }
button {
  background: #5C3A1E;
  color: #F0B27A;
  width: 80px;
  height: 16px;
}
button:hover { background: #7C5A2E; }
```

**Extend `TesseraScreen` in your screen class:**
```java
public class MyScreen extends TesseraScreen {

    private TesseraPanel root;

    public MyScreen() {
        super(Component.literal("My Screen"));
    }

    @Override
    protected void init() {
        TesseraModel model = TesseraModel.of(Map.of(
            "player.name", Minecraft.getInstance().player.getName().getString()
        ));

        root = TesseraTemplateRenderer.build(
            TesseraTemplate.load("yourmod:ui/my_screen"),
            model,
            Map.of("openSettings", this::openSettings),
            x, y, width, height
        );
        root.layout();
    }

    @Override
    protected TesseraPanel tesseraRoot() { return root; }
}
```

`TesseraScreen` wires up `render()`, `mouseClicked()`, `keyPressed()`, and `mouseScrolled()` automatically.

## Data binding

Pass dynamic values via `TesseraModel`:

```java
TesseraModel model = TesseraModel.of(Map.of(
    "player.name",  player.getName().getString(),
    "player.level", String.valueOf(playerLevel),
    "items.count",  String.valueOf(items.size())
));
```

In HTML, use `{{ key }}` for simple interpolation or ternary expressions:

```html
<label>{{ player.name }} — Level {{ player.level }}</label>
<label>{{ items.count == 0 ? "Empty" : items.count + " items" }}</label>
```

### Loops (`v-for`)

```html
<col v-for="item in items">
  <label>{{ item.name }}</label>
  <badge>{{ item.rarity }}</badge>
</col>
```

### Conditional display (`v-if`)

```html
<row v-if="player.isAdmin">
  <button onclick="openAdmin">Admin panel</button>
</row>
```

## Supported tags

### Layout

| Tag | Description |
|-----|-------------|
| `<col>` / `<div>` | Vertical flex container (column direction) |
| `<row>` | Horizontal flex container |
| `<grid cols="N">` | Grid layout with N columns |
| `<section>`, `<article>`, `<main>`, `<nav>`, `<header>`, `<footer>`, `<aside>`, `<form>` | Block containers (same as `<div>`) |
| `<hr>` | Horizontal separator |

### Text & rich content

| Tag | Description |
|-----|-------------|
| `<h1>` … `<h6>` | Headings (font sizes 14 → 7px) |
| `<p>` | Paragraph |
| `<label>` | Inline text label |
| `<span>` | Inline wrapper |
| `<strong>`, `<b>` | Bold text |
| `<em>`, `<i>` | Italic text |
| `<a href="...">` | Clickable text link |
| `<badge>` | Pill-shaped label |

### Inputs & interactive

| Tag | Description |
|-----|-------------|
| `<button onclick="handler">` | Clickable button |
| `<input id="id" placeholder="...">` | Single-line text input |
| `<textarea id="id" placeholder="...">` | Multi-line text area |
| `<checkbox id="id">` | Toggle checkbox |
| `<slider id="id" min="0" max="100">` | Value slider |
| `<select>` | Dropdown selector |

### Tabs

```html
<tabs>
  <tab label="General">
    <col>…</col>
  </tab>
  <tab label="Advanced">
    <col>…</col>
  </tab>
</tabs>
```

### Item slots

| Tag | Attributes | Description |
|-----|------------|-------------|
| `<item-slot>` | `size`, `item="namespace:id"`, `show-count` | Displays a single Minecraft item |

```html
<row>
  <item-slot size="24" item="minecraft:diamond" show-count="false"/>
  <item-slot size="24" item="minecraft:gold_ingot"/>
</row>
```

### Templates & slots (component system)

Define reusable components with `<template>` and compose them with named `<slot>` injection points:

```html
<template name="card">
  <col class="card">
    <row class="card-header">
      <slot name="title"/>
    </row>
    <col class="card-body">
      <slot/>            <!-- default slot -->
    </col>
  </col>
</template>

<row>
  <card>
    <h3 slot="title">Alpha</h3>
    <p>Content goes here.</p>
  </card>
  <card>
    <h3 slot="title">Beta</h3>
    <p>More content.</p>
  </card>
</row>
```

### Drag & Drop (HTML)

Mark any `<div>` as draggable with `draggable="true"` and a `drag-payload` value. The payload can be dropped onto `TesseraDropZone` targets registered in Java.

```html
<row>
  <div class="drag-red"   draggable="true" drag-payload="red">Red</div>
  <div class="drag-blue"  draggable="true" drag-payload="blue">Blue</div>
</row>
```

```java
dropZone.dropZone(new TesseraDropZone() {
    @Override public boolean accepts(Object payload) { return payload instanceof String; }
    @Override public void    onDrop(Object payload)  { /* handle drop */ }
    @Override public Rect    dropBounds()            { return dropZone.bounds(); }
});
```

### Media & data

| Tag | Description |
|-----|-------------|
| `<img src="namespace:path">` | Image / texture |
| `<icon src="name">` | Icon from `textures/gui/icons/<name>.png` |
| `<table>`, `<thead>`, `<tbody>`, `<tfoot>`, `<tr>`, `<td>`, `<th>` | Table layout |
| `<ul>`, `<li>` | List with bullet points |

## CSS

### Selectors

```css
/* Tag */
button { }

/* Class */
.my-panel { }

/* Descendant combinator */
.toolbar button { }

/* Child combinator */
.toolbar > button { }

/* Multiple selectors */
h1, h2, h3 { }

/* Pseudo-states */
button:hover    { }
button:active   { }
button:focus    { }
button:disabled { }
```

### Supported properties

**Layout**

| Property | Values |
|----------|--------|
| `display` | `flex`, `none` |
| `flex-direction` | `row`, `column` |
| `justify-content` | `flex-start`, `center`, `flex-end`, `space-between`, `space-around` |
| `align-items` | `flex-start`, `center`, `flex-end`, `stretch` |
| `align-self` | same as `align-items` |
| `flex` | shorthand (`flex-grow flex-shrink flex-basis`) |
| `flex-grow` | number |
| `flex-shrink` | number |
| `flex-basis` | px value |
| `flex-wrap` | `wrap`, `nowrap` |
| `order` | integer |
| `z-index` | integer |
| `gap` | px |
| `padding` | px (1–4 values) |
| `margin` | px (1–4 values) |
| `margin-top` | px or `auto` (pushes element to bottom in column) |
| `width`, `height` | px or `%` |
| `min-width`, `max-width` | px |
| `box-sizing` | `border-box`, `content-box` |
| `overflow` | `hidden`, `scroll`, `auto`, `visible` |
| `position` | `relative`, `absolute` |

**Visual**

| Property | Values |
|----------|--------|
| `background`, `background-color` | color |
| `color` | color |
| `border` | `<px> [solid] [color]` |
| `border-color` | color |
| `border-top-color`, `border-bottom-color`, `border-left-color`, `border-right-color` | color |
| `opacity` | `0.0` – `1.0` |
| `corner-dot-size` | px |
| `corner-dot-color` | color |

**Text**

| Property | Values |
|----------|--------|
| `font-family` | font name (registered in resource pack) |
| `font-size` | px |
| `font-weight` | `bold`, `normal` |
| `text-align` | `left`, `center`, `right` |
| `text-transform` | `uppercase`, `lowercase`, `none` |

### Color formats

```css
/* Named colors */
color: white;
color: black;
color: red;
color: transparent;

/* Hex — all formats supported */
color: #RGB;        /* short 3-digit  → #RRGGBB, alpha = FF */
color: #RGBA;       /* short 4-digit  → #RRGGBBAA */
color: #RRGGBB;     /* standard 6-digit, alpha = FF */
color: #AARRGGBB;   /* full 8-digit with explicit alpha */

/* rgb() */
color: rgb(255, 128, 0);
```

Available named colors: `white`, `black`, `red`, `green`, `lime`, `blue`, `yellow`, `gray`/`grey`, `silver`, `orange`, `purple`, `cyan`, `magenta`, `pink`, `brown`, `navy`, `teal`, `gold`, `copper`, `maroon`, `olive`, `aqua`, `fuchsia`, `indigo`, `violet`, `coral`, `salmon`, `khaki`, `beige`, `transparent`.

### CSS variables

```css
:root {
  --accent: #B87333;
  --bg:     #1A1208;
}

.panel { background: var(--bg); }
.title { color: var(--accent); }
```

### `@media` queries

Adapt the layout based on the GUI-scaled viewport width (changes with Minecraft's GUI Scale setting):

```css
/* Default: mobile-first */
.sidebar { display: none; }

/* Show sidebar on wide viewports */
@media (min-width: 521px) {
  .sidebar { display: flex; }
}

/* Compact layout on narrow viewports */
@media (max-width: 520px) {
  .toolbar-extra { display: none; }
}
```

`TesseraTemplateRenderer` resolves the active media rules automatically using `Window#getGuiScaledWidth()`.

## Localisation (i18n)

TesseraUI hooks into Minecraft's built-in translation system.  
**Your mod** supplies the lang files — TesseraUI just resolves them at render time using `I18n.get(key)`, which searches all loaded mods' lang files automatically.

### 1. Add lang files in your mod

```
assets/yourmod/lang/en_us.json
assets/yourmod/lang/fr_fr.json
assets/yourmod/lang/de_de.json
…
```

```json
// assets/yourmod/lang/en_us.json
{
  "ui.yourmod.title":       "My Settings",
  "ui.yourmod.confirm":     "Confirm",
  "ui.yourmod.cancel":      "Cancel",
  "ui.yourmod.greeting":    "Hello",
  "ui.yourmod.items.empty": "No items",
  "ui.yourmod.items.count": "items"
}
```

```json
// assets/yourmod/lang/fr_fr.json
{
  "ui.yourmod.title":       "Mes paramètres",
  "ui.yourmod.confirm":     "Confirmer",
  "ui.yourmod.cancel":      "Annuler",
  "ui.yourmod.greeting":    "Bonjour",
  "ui.yourmod.items.empty": "Aucun élément",
  "ui.yourmod.items.count": "éléments"
}
```

### 2. Use `data-i18n` in your HTML templates

The `data-i18n` attribute replaces an element's text content with the active translation.  
The text content in the HTML serves as fallback if the key is absent from the active lang file.

```html
<col>
  <h2 data-i18n="ui.yourmod.title">My Settings</h2>

  <row>
    <button data-i18n="ui.yourmod.confirm">Confirm</button>
    <button data-i18n="ui.yourmod.cancel">Cancel</button>
  </row>
</col>
```

Works on any text-bearing element: `button`, `label`, `p`, `h1`–`h6`, `badge`, `span`, `strong`, `em`, `a`, `li`.

### 3. Use `{{ t:key }}` for inline mixed content

When you need to mix a translation with a dynamic value:

```html
<!-- "Hello, Steve!" -->
<label>{{ t:ui.yourmod.greeting }}, {{ player.name }} !</label>

<!-- "42 items"  or  "No items" -->
<label>{{ items.count == 0 ? t:ui.yourmod.items.empty : items.count + " " + t:ui.yourmod.items.count }}</label>

<!-- Ternary with two translation keys — use " : " (with spaces) as separator -->
<label>{{ player.level > 10 ? t:ui.yourmod.rank.expert : t:ui.yourmod.rank.novice }}</label>
```

> **Note:** In ternary expressions, use ` : ` (colon surrounded by spaces) as the true/false separator when either branch contains a `t:` key. Plain ternaries without `t:` keys work with or without spaces.

### Architecture summary

| What | Where |
|---|---|
| Translation mechanism | TesseraUI (built-in, nothing to configure) |
| Translation keys (`data-i18n`, `{{ t:key }}`) | Your HTML templates |
| Lang files (`en_us.json`, `fr_fr.json`, …) | Your mod's `assets/yourmod/lang/` |
| Fallback text if key missing | The text content written in the HTML |

## Font families

Register custom fonts in a resource pack under the `tesseraui` namespace:

```
assets/tesseraui/font/fantasy.json
assets/tesseraui/font/mono.json
```

Declare `font-family: fantasy` in CSS. Falls back to the default Minecraft font if the resource is absent.

## Programmatic API

Build panels directly without a template:

```java
TesseraPanel panel = TesseraPanel.column(x, y, 120, 80)
    .background(0xFF1A1208)
    .padding(8)
    .gap(4);

panel.add(new TesseraLabel(0, 0, 100, 12, "Hello world")
    .color(TesseraPalette.CREAM));
panel.add(new TesseraButton(0, 0, 60, 14)
    .label("OK")
    .onClick(this::onOk));

panel.layout();
```

### TesseraItemSlot

Displays a single Minecraft item with an optional inventory picker.

```java
// Display only
TesseraItemSlot slot = new TesseraItemSlot(0, 0, 32)
    .item(new ItemStack(Items.DIAMOND));

// Clickable — opens a floating inventory picker overlay
TesseraItemSlot slot = new TesseraItemSlot(0, 0, 32)
    .item(new ItemStack(Items.DIAMOND))
    .inventoryPicker(true);           // click to pick from player inventory

// With a custom callback (e.g. to persist the choice)
slot.onItemPicked(stack -> {
    myConfig.setItem(stack);
    myConfig.save();
});
```

When `inventoryPicker(true)` is set and no `onItemPicked` callback is supplied, the picked item simply replaces the slot's current item.

### TesseraItemGrid

A compact drag-and-drop item grid backed by a flat `ItemStack[]`.

```java
// 4 columns × 2 rows, each slot 22 px
TesseraItemGrid grid = new TesseraItemGrid(0, 0, 4, 2, 22);
grid.setItem(0, new ItemStack(Items.DIAMOND));
grid.setItem(1, new ItemStack(Items.GOLD_INGOT, 3));
// slots are addressed row-major: index = row * cols + col
panel.add(grid);
```

Items can be dragged between slots in the grid out of the box — no extra wiring required.

### TesseraInventoryPicker

A static floating overlay that shows the player's full inventory (main 3×9 + hotbar 1×9). It is opened automatically by `TesseraItemSlot.inventoryPicker(true)` but can also be triggered manually:

```java
// Open near a custom widget; picker clamps to screen bounds automatically.
TesseraInventoryPicker.open(anchorX, anchorY, stack -> {
    // called with a copy of the chosen ItemStack
    mySlot.item(stack);
});
```

Clicking outside the picker panel closes it without picking anything. The picker is rendered by `TesseraScreen.renderTesseraOverlays()` — no extra wiring needed when extending `TesseraScreen`.

## Color palette

`TesseraPalette` provides a copper-patina design system:

| Constant | Role |
|----------|------|
| `BG0` | Darkest background |
| `BG1` | Panel background |
| `BG2` | Elevated surface |
| `COPPER` | Primary accent |
| `COPPER_HI` | Hover / highlight |
| `COPPER_LO` | Subtle border |
| `CREAM` | Primary text |
| `CREAM_DIM` | Secondary text |
| `GOOD` | Success |
| `WARN` | Warning |
| `DANGER` | Error / danger |

## Hot reload (development)

During development, call `TesseraHotReload.enable()` to automatically reload templates from disk whenever a file changes — no restart required.

## License

LGPL-2.1 — you may use TesseraUI in closed-source mods without open-sourcing your mod code.
