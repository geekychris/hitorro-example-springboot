# JSON Type System - Hierarchical Explorer with Nested Types ✅

## Enhancement Complete

The Type System Explorer now shows **comprehensive hierarchical type information** including nested types, primitive types, dynamic fields, groups, and tags - exactly as defined in the JSON Type System.

## Features Added

### Backend (`JVSController.java`)

#### Enhanced Type Field Information

**New `TypeField` properties:**
- `boolean vector` - Array/collection field
- `boolean i18n` - Internationalized field
- `boolean dynamic` - Computed/dynamic field
- `DynamicInfo dynamicInfo` - Dynamic mapper details
- `List<GroupInfo> groups` - Enrichment/index groups
- `boolean isPrimitive` - Is this a primitive type
- `String primitiveType` - Primitive type (string, long, etc.)

#### New DTOs

**`DynamicInfo`:**
- `className` - Full Java class name of the dynamic mapper
- `dependsOn` - List of field paths this dynamic field depends on (e.g., `[".lang", ".text"]`)

**`GroupInfo`:**
- `name` - Group name (e.g., "index", "enrich")
- `method` - Index/enrichment method (e.g., "text", "long")
- `tags` - Tags for conditional processing (e.g., `["ner"]`, `["basic"]`, `["answers"]`)

#### Implementation

- Uses **reflection** to access Field internals (groups, dynamic mappers)
- Recursively explores nested type definitions
- Detects primitive types vs complex types
- Extracts dynamic field dependencies
- Captures group tags for enrichment control

### Frontend (`TypeSystemPage.tsx`)

#### New `FieldDisplay` Component

**Hierarchical field visualization:**
- ✅ **Expandable/collapsible** nested types (ChevronRight/ChevronDown)
- ✅ **Auto-expands** first 2 levels
- ✅ **Lazy loads** nested type definitions on expand
- ✅ **Color-coded borders**:
  - Orange (dynamic fields)
  - Green (primitive types)
  - Gray (complex types)
- ✅ **Indented hierarchy** showing type nesting
- ✅ **Alternating backgrounds** for depth clarity

#### Visual Badges

**Field Attributes:**
- 🟢 **Primitive** badge (e.g., "string", "long", "boolean")
- 🔵 **vector** badge with layers icon
- 🟣 **i18n** badge
- 🟡 **⚡ dynamic** badge

**Group Information:**
- 🔵 Method badge (e.g., "text", "long", "textmarkup")
- 🔴 Tag badges (e.g., "ner", "basic", "segmented", "answers")

#### Details Panel

Click "Details" button to show:

**Dynamic Fields:**
- Mapper class name (e.g., "NERMarkupMapper", "Json2HTMLScrubbedJson")
- Dependencies (e.g., "Depends on: `.lang` `.text`")
- Yellow background highlighting

**Groups:**
- Group name and method
- All associated tags
- White cards with colored badges

## Example: Exploring `core_mlselem`

### Type Structure

```
core_mlselem
├── lang (core_string) [primitive: string]
├── text (core_string) [i18n, primitive: string]
├── clean (core_string) [i18n, dynamic, primitive: string]
│   ├── Dynamic: Json2HTMLScrubbedJson
│   │   └── Depends on: .text
│   └── Groups:
│       └── index (method: text, tags: [basic])
├── segmented (core_string) [vector, i18n, dynamic, primitive: string]
│   ├── Dynamic: SentenceSegmenter
│   │   └── Depends on: .segmented_span, .clean
│   └── Groups:
│       ├── index (method: text, tags: [basic])
│       └── enrich (method: text, tags: [segmented])
└── segmented_ner (core_string) [vector, i18n, dynamic, primitive: string]
    ├── Dynamic: NERMarkupMapper
    │   └── Depends on: .lang, .segmented
    └── Groups:
        ├── index (method: textmarkup, tags: [advanced])
        └── enrich (method: text, tags: [ner])
```

### In the UI

When you click on `core_mlselem` and expand fields, you'll see:

**segmented_ner field:**
- **Name**: segmented_ner
- **Type**: core_string (with expand button)
- **Badges**: 
  - 🟢 primitive: string
  - 🔵 vector
  - 🟣 i18n  
  - 🟡 ⚡ dynamic
- **Details** button → Click to reveal:
  - **Dynamic Mapper**: NERMarkupMapper
  - **Depends on**: `.lang` `.segmented`
  - **Groups**:
    - **index** [textmarkup] tags: [advanced]
    - **enrich** [text] tags: [ner]

### Nested Type Navigation

Example with `core_mls`:

```
core_mls
└── mls (mlselem) [vector] ← Click expand arrow
    ├── lang (core_string) [primitive: string]
    ├── text (core_string) [i18n, primitive: string]
    ├── clean (core_string) [i18n, dynamic]...
    └── segmented_ner (core_string) [vector, i18n, dynamic]...
```

## Usage

### Start the Application

```bash
# Backend
cd hitorro-example-springboot
./run.sh

# Frontend
cd react-app
npm run dev
```

### Explore Types

1. Go to **Type System** tab
2. Scroll to **"JSON Type System - Type Explorer"**
3. Click any type (e.g., `core_mlselem`, `core_mls`, `core_sysobject`)
4. See **all fields** with their attributes
5. Click **expand arrows** (▶/▼) to explore nested types
6. Click **"Details"** to see dynamic mappers and groups
7. View **tags** to understand enrichment requirements

### Understanding Dynamic Fields

When you see a field like `segmented_ner`:
- **Dynamic badge**: Computed field, not stored
- **Depends on**: Shows what fields it needs (`.lang`, `.segmented`)
- **Groups with tags**: Shows when it's computed
  - **enrich** group with **ner** tag → Generated during `JVS2JVSEnrichMapper` with `"ner"` tag!

## Technical Details

### Field Discovery

- Scans `HT_BIN/config/types/` for `.json` files
- Parses type definitions into `Type` objects
- Uses reflection to access private `fields` map
- Recursively loads nested type definitions on-demand

### Lazy Loading

- Nested types loaded only when expanded
- React Query caches type definitions
- Prevents excessive API calls
- Smooth user experience

### Visual Hierarchy

- Indent increases by 1.5rem per level
- Alternating backgrounds (white/gray)
- Left border shows nesting depth
- Colored borders indicate field types

## Status

✅ Backend extracts comprehensive type information  
✅ Frontend displays hierarchical nested types  
✅ Dynamic fields with dependencies shown  
✅ Groups and tags visualized  
✅ Primitive types detected and labeled  
✅ Expandable/collapsible navigation  
✅ Lazy loading of nested types  
✅ Beautiful, intuitive UI  

The Type System Explorer now provides **complete visibility** into the JSON Type System structure, making it easy to understand type hierarchies, field attributes, dynamic computations, and enrichment requirements!
