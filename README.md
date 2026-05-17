# TesseraUI

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-E04E14?style=flat-square)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-LGPL--3.0-4A90D9?style=flat-square)](LICENSE)
[![CurseForge](https://img.shields.io/badge/CurseForge-TesseraUI-F16436?style=flat-square&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/tesseraui)
[![GitHub Stars](https://img.shields.io/github/stars/Blushister/TesseraUI?style=flat-square&logo=github&color=B87333)](https://github.com/Blushister/TesseraUI)
[![Wiki](https://img.shields.io/badge/Wiki-read%20the%20docs-805AD5?style=flat-square&logo=gitbook&logoColor=white)](https://github.com/Blushister/TesseraUI/wiki)

Write Minecraft GUI screens in HTML and CSS. TesseraUI parses them at runtime and renders native widgets — no JavaScript, no browser engine.

Everything that's painful to build in NeoForge GUI (flexbox layout, hover transitions, `@keyframes` animations, drag & drop, item pickers, virtual lists, i18n) is handled for you. Use HTML templates or the programmatic Java API — your choice.

---

## Installation

### 1. Download the jar

Grab the latest `tesseraui-x.x.x.jar` from the [Releases](https://github.com/Blushister/TesseraUI/releases) page or from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/tesseraui).

### 2. Add it to your mod

Drop the jar in your `libs/` folder, then in `build.gradle`:

```groovy
dependencies {
    implementation files('libs/tesseraui-1.1.jar')
}
```

### 3. Declare the dependency

`src/main/resources/META-INF/neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
    modId        = "tesseraui"
    type         = "required"
    versionRange = "[1.0,)"
    ordering     = "NONE"
    side         = "CLIENT"
```

### Via CurseForge Maven

If you prefer Gradle dependency management:

```groovy
repositories {
    maven { url "https://cursemaven.com" }
}
dependencies {
    implementation fg.deobf("curse.maven:tesseraui-<file-id>:latest")
}
```

Replace `<file-id>` with the file ID from the CurseForge project page.

---

## Quick start — HTML template

`assets/yourmod/ui/my_screen.html`
```html
<col>
  <h2 data-i18n="ui.yourmod.title">Settings</h2>
  <p>Hello, {{ player.name }}!</p>

  <row>
    <button onclick="save"   data-i18n="ui.yourmod.save">Save</button>
    <button onclick="cancel" data-i18n="ui.yourmod.cancel">Cancel</button>
  </row>
</col>
```

`assets/yourmod/ui/my_screen.css`
```css
col   { background: #1A1208; padding: 12px; gap: 8px; }
h2    { color: #F0B27A; font-size: 10px; }
p     { color: #F3E7D3; font-size: 7px; }

button {
  background: #5C3A1E;
  color: #F0B27A;
  width: 80px; height: 16px;
  transition: background 200ms ease-out;
}
button:hover { background: #7C5A2E; }
```

```java
public class MyScreen extends TesseraScreen {

    private TesseraPanel root;

    public MyScreen() { super(Component.literal("My Screen")); }

    @Override
    protected void init() {
        TesseraModel model = TesseraModel.of(Map.of(
            "player.name", Minecraft.getInstance().player.getName().getString()
        ));
        int pw = Math.min(width, 300), ph = Math.min(height, 200);
        root = TesseraTemplateRenderer.build(
            TesseraTemplate.load("yourmod:ui/my_screen"),
            model,
            Map.of("save", this::onSave, "cancel", this::onCancel),
            (width - pw) / 2, (height - ph) / 2, pw, ph
        );
        root.layout();
    }

    @Override protected TesseraPanel tesseraRoot() { return root; }
}
```

---

## Quick start — programmatic Java API

No HTML file needed. Every feature is available directly in Java.

```java
// Keyframe animation
TesseraKeyframes pulse = TesseraKeyframes.builder("pulse")
    .from(s -> { s.background = 0xFF1a2a1a; s.borderColor = 0xFF22c55e; })
    .at(50, s -> { s.background = 0xFF14532d; s.borderColor = 0xFF4ade80; })
    .to(s ->   { s.background = 0xFF1a2a1a; s.borderColor = 0xFF22c55e; })
    .build();

TesseraPanel card = TesseraPanel.column(x, y, 120, 60)
    .padding(8).gap(4)
    .animate(pulse, 1500, TesseraEasing.EASE_IN_OUT)          // infinite loop
    .hoverTransition("border-color", 150, TesseraEasing.EASE_OUT, 0xFF22c55e, 0xFF4ade80);

// v-for / v-if helpers
panel.addFor(playerList, p ->
    new TesseraLabel(0, 0, 120, 10, p.getName()).color(TesseraPalette.CREAM));

panel.addIf(isAdmin, () -> buildAdminPanel());
```

---

## Feature overview

| Feature | HTML | Java API |
|---------|:----:|:--------:|
| Flexbox & grid layout | ✓ | ✓ |
| CSS variables, `@media` queries | ✓ | — |
| Hover transitions | ✓ | ✓ |
| `@keyframes` animations | ✓ | ✓ |
| Data binding `{{ }}`, `v-for`, `v-if` | ✓ | ✓ |
| Component system `<template>` / `<slot>` | ✓ | — |
| i18n `data-i18n` / `{{ t:key }}` | ✓ | ✓ |
| Drag & Drop | ✓ | ✓ |
| Item slots & inventory picker | ✓ | ✓ |
| Virtual list (large datasets) | ✓ | ✓ |
| Tabs | ✓ | ✓ |
| Hot reload from disk | ✓ | ✓ |

---

## Documentation

Full reference on the **[GitHub Wiki](https://github.com/Blushister/TesseraUI/wiki)**:

| Page | |
|------|--|
| [Getting Started](https://github.com/Blushister/TesseraUI/wiki/Getting-Started) | Installation and first screen |
| [HTML Tags](https://github.com/Blushister/TesseraUI/wiki/HTML-Tags) | All supported elements |
| [CSS Reference](https://github.com/Blushister/TesseraUI/wiki/CSS-Reference) | Every supported property |
| [CSS Animations](https://github.com/Blushister/TesseraUI/wiki/CSS-Animations) | `transition` and `@keyframes` |
| [Layout System](https://github.com/Blushister/TesseraUI/wiki/Layout-System) | Flexbox and grid |
| [Data Binding](https://github.com/Blushister/TesseraUI/wiki/Data-Binding) | `{{ }}`, `v-for`, `v-if` |
| [Programmatic API](https://github.com/Blushister/TesseraUI/wiki/Programmatic-API) | Full Java API |
| [Item Slots](https://github.com/Blushister/TesseraUI/wiki/Item-Slots) | Item display and inventory picker |
| [Drag and Drop](https://github.com/Blushister/TesseraUI/wiki/Drag-and-Drop) | Draggable widgets |
| [Localization](https://github.com/Blushister/TesseraUI/wiki/Localization) | i18n integration |
| [Hot Reload](https://github.com/Blushister/TesseraUI/wiki/Hot-Reload) | Development workflow |

---

## License

[LGPL-3.0](LICENSE) — you can use TesseraUI in any mod (open-source or closed-source, free or paid) without restrictions. If you modify TesseraUI itself, those modifications must be published under LGPL-3.0.
