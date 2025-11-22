package com.github.gotify.messages

import com.github.gotify.client.model.Message

internal object Extras {
    fun useMarkdown(message: Message): Boolean = useMarkdown(message.extras)

    fun useMarkdown(extras: Map<String, Any>?): Boolean {
        if (extras == null) {
            return false
        }

        val display: Any? = extras["client::display"]
        if (display !is Map<*, *>) {
            return false
        }

        return "text/markdown" == display["contentType"]
    }

    fun useHtml(message: Message): Boolean = useHtml(message.extras)

    fun useHtml(extras: Map<String, Any>?): Boolean {
        if (extras == null) {
            return false
        }

        val display: Any? = extras["client::display"]
        if (display !is Map<*, *>) {
            return false
        }

        return "text/html" == display["contentType"]
    }
    
    // Función auxiliar para detectar si un mensaje contiene HTML
    fun containsHtmlTags(message: String): Boolean {
        val htmlPattern = "<\\s*[a-zA-Z][^>]*>".toRegex()
        return htmlPattern.containsMatchIn(message)
    }

    fun hasComplexHtml(message: Message): Boolean {
        val content = message.message.lowercase()
        // Lista de tags que requieren WebView para renderizado completo
        val complexTags = listOf(
            "<details", "<summary", "<div", "<span", "<section", "<article",
            "<header", "<footer", "<nav", "<aside", "<main", "<figure",
            "<figcaption", "<table", "<thead", "<tbody", "<tr", "<td", "<th",
            "<ul", "<ol", "<li", "<dl", "<dt", "<dd", "<form", "<input",
            "<textarea", "<button", "<select", "<option", "<label", "<fieldset",
            "<legend", "<progress", "<meter", "<canvas", "<svg", "<iframe",
            "<embed", "<object", "<param", "<video", "<audio", "<source",
            "<track", "<mark", "<time", "<data", "<output", "<ruby", "<rt",
            "<rp", "<bdi", "<bdo", "<wbr", "<dialog", "<slot", "<template"
        )
        
        // Atributos que indican HTML complejo
        val complexAttributes = listOf(
            "style=", "class=", "id=", "onclick", "onload", "data-",
            "contenteditable", "draggable", "hidden", "spellcheck",
            "tabindex", "title", "dir", "lang"
        )
        
        // Si contiene cualquier tag complejo, usar WebView
        val hasComplexTags = complexTags.any { tag -> content.contains(tag) }
        val hasComplexAttrs = complexAttributes.any { attr -> content.contains(attr) }
        
        return hasComplexTags || hasComplexAttrs
    }

    fun <T> getNestedValue(clazz: Class<T>, extras: Map<String, Any>?, vararg keys: String): T? {
        var value: Any? = extras

        keys.forEach { key ->
            if (value == null) {
                return null
            }

            value = (value as Map<*, *>)[key]
        }

        if (!clazz.isInstance(value)) {
            return null
        }

        return clazz.cast(value)
    }
}
