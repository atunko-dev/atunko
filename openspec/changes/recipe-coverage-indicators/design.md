## Design

### Covered Recipe Computation

`TuiController.coveredRecipes()` computes a `Set<String>` of recipe names that are
transitively included by any selected composite recipe. The computation walks the
`recipeList` tree recursively via `collectSubRecipeNames()`.

### Included-In Lookup

`TuiController.includedIn(recipeName)` returns a `List<String>` of selected composite
display names that contain the given recipe (directly or transitively). Uses
`containsSubRecipe()` helper for recursive tree search.

### Visual Indicators

`RecipeListRenderer` receives the `coveredRecipes` set and renders:
- `[x]` — explicitly selected
- `[✓]` — sub-recipe of an expanded selected composite (included)
- `[≈]` — top-level recipe covered by a selected composite
- `[ ]` — not selected, not covered

Covered-but-unselected recipes are dimmed. Included sub-recipes use muted green.

### Detail Panel

Both `BrowserView.renderDetailPanel()` and `DetailView.renderRecipeDetail()` show an
"Included in:" section (yellow text) when the highlighted recipe is covered.
