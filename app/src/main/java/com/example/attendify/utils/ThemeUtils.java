package com.example.attendify.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.google.android.material.color.MaterialColors;

/**
 * Utility class for theme-related operations
 */
public class ThemeUtils {

    /**
     * Checks if the current theme is in dark mode
     * 
     * @param context The context
     * @return true if in dark mode, false otherwise
     */
    public static boolean isDarkTheme(@NonNull Context context) {
        return (context.getResources().getConfiguration().uiMode & 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Gets a color from the Material theme
     * 
     * @param context The context
     * @param colorAttributeResId The color attribute resource ID
     * @return The color value
     */
    @ColorInt
    public static int getThemeColor(@NonNull Context context, int colorAttributeResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttributeResId, typedValue, true);
        return typedValue.data;
    }
    
    /**
     * Creates a divider decoration appropriate for the current theme
     * 
     * @param context The context
     * @return A RecyclerView item decoration
     */
    public static RecyclerView.ItemDecoration getListDividerDecoration(@NonNull Context context) {
        DividerItemDecoration divider = new DividerItemDecoration(
                context, DividerItemDecoration.VERTICAL);
        
        Drawable dividerDrawable = ContextCompat.getDrawable(context, R.drawable.list_divider);
        if (dividerDrawable != null) {
            // Apply theme-appropriate color to divider
            int dividerColor = MaterialColors.getColor(
                    context, com.google.android.material.R.attr.colorOutline, 
                    ContextCompat.getColor(context, isDarkTheme(context) ? 
                            R.color.gray_700 : R.color.gray_300));
                            
            dividerDrawable.setTint(dividerColor);
            divider.setDrawable(dividerDrawable);
        }
        
        return divider;
    }
    
    /**
     * Applies a theme-appropriate background tint to a drawable
     * 
     * @param context The context
     * @param drawable The drawable to tint
     * @param backgroundAttrResId The attribute resource ID for the background color
     */
    public static void applyBackgroundTint(@NonNull Context context, 
                                         @NonNull Drawable drawable, 
                                         int backgroundAttrResId) {
        int color = MaterialColors.getColor(
                context, backgroundAttrResId, 
                ContextCompat.getColor(context, R.color.surface));
        drawable.setTint(color);
    }
} 