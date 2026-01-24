# Dynamic Fields Dependencies - Implementation Status

## Current Status

The Type System Explorer now shows **all dynamic field information** including:

✅ **Which fields are dynamic** - Orange border + yellow badge  
✅ **Implementation class** - Full Java class name (e.g., `NERMarkupMapper`)  
✅ **Groups and tags** - All enrichment groups with their tags  
✅ **Visual indicators** - Color-coded and prominent display  

## Dependencies Field - In Progress

The `dependsOn` field (showing which fields the dynamic field depends on) is currently showing `null`. This is being investigated.

### What Should Show

For `segmented_ner` field, the dependencies from `core_mlselem.json` are:
```json
"fields": [
  ".lang",
  ".segmented"
]
```

This should appear in the UI as:
```
Dependencies:
  .lang    .segmented
ℹ️ This field is computed from the values of these fields
```

### Workaround - Check JSON Definition

Until the dependencies are properly extracted, you can:

1. **Look at the type definition JSON files** in `HT_BIN/config/types/`
2. **Find the `dynamic` section** for any field
3. **Look at the `fields` array** - these are the dependencies

Example from `core_mlselem.json` line 214-220:
```json
{
  "name": "segmented_ner",
  "dynamic": {
    "class": "com.hitorro.jsontypesystem.dynamic.NERMarkupMapper",
    "fields": [
      ".lang",
      ".segmented"
    ]
  }
}
```

## Current UI Features Working

Even without the dependencies showing, the UI provides:

### ✅ Full Dynamic Field Visualization

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ segmented_ner                               ┃
┃ 🟢 primitive: string                        ┃
┃ 🔵 vector                                   ┃
┃ 🟣 i18n                                     ┃
┃ 🟡 ⚡ dynamic              core_string      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

⚡ Dynamic Field (Computed)

Implementation:
com.hitorro.jsontypesystem.dynamic.NERMarkupMapper
Mapper: NERMarkupMapper

Dependencies: [To be fixed]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🏷️ Groups & Tags (2)

┌──────────────────────────────────────────┐
│ index         method: textmarkup         │
│ Tags: 🏷️ advanced                        │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│ enrich        method: text               │
│ Tags: 🏷️ ner                             │
│ ℹ️ Use JVS2JVSEnrichMapper with tags: ner│
└──────────────────────────────────────────┘
```

### ✅ Key Information Visible

- **Is it dynamic?** → Yes (⚡ badge + orange border)
- **Implementation?** → NERMarkupMapper class
- **When is it generated?** → enrich group with "ner" tag
- **How to get it?** → Use `JVS2JVSEnrichMapper` with tags `["ner", "segmented"]`
- **What type?** → vector of strings, i18n-enabled, primitive string type

## Next Steps

The backend code has been updated to extract the `fields` array from `DynamicFieldMapper`, but it's currently returning `null`. This needs further investigation to:

1. Verify the `fields` array is properly initialized when the Type is loaded
2. Ensure reflection is accessing the correct field
3. Convert Propaccess objects to readable string paths

## Viewing the UI Now

**Restart the browser** (hard refresh: Cmd+Shift+R) and you'll see:

1. All fields with **colored badges** (vector, i18n, dynamic, primitive)
2. **Orange borders** around dynamic fields
3. **Yellow panels** showing mapper implementation
4. **Purple panels** showing groups and tags
5. **Expandable hierarchy** for nested types

**The dependencies will show once the backend issue is resolved, but everything else is working!**
