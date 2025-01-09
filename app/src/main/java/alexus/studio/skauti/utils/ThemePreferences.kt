package alexus.studio.skauti.utils

import android.content.Context

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    fun isDarkTheme(): Boolean {
        return prefs.getBoolean("is_dark_theme", false)
    }
    
    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }
} 