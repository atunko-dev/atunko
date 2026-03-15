package io.github.atunkodev.web.view;

import io.github.atunkodev.core.recipe.RecipeInfo;

/**
 * Wraps a {@link RecipeInfo} with a position-unique path so that the same recipe can appear at
 * multiple locations in a {@code TreeGrid} without violating Vaadin's uniqueness constraint.
 */
public record TreeNode(RecipeInfo recipe, String path) {}
