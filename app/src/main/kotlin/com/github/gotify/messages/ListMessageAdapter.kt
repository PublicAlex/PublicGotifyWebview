package com.github.gotify.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.text.Html
import android.text.format.DateUtils
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.ImageLoader
import coil.load
import com.github.gotify.MarkwonFactory
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.client.model.Message
import com.github.gotify.databinding.MessageItemBinding
import com.github.gotify.databinding.MessageItemCompactBinding
import com.github.gotify.messages.provider.MessageWithImage
import io.noties.markwon.Markwon
import java.text.DateFormat
import java.util.Date
import org.threeten.bp.OffsetDateTime

internal class ListMessageAdapter(
    private val context: Context,
    private val settings: Settings,
    private val imageLoader: ImageLoader,
    private val delete: Delete
) : ListAdapter<MessageWithImage, ListMessageAdapter.ViewHolder>(DiffCallback) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val markwon: Markwon = MarkwonFactory.createForMessage(context, imageLoader)

    private val timeFormatRelative =
        context.resources.getString(R.string.time_format_value_relative)
    private val timeFormatPrefsKey = context.resources.getString(R.string.setting_key_time_format)

    private var messageLayout = 0

    init {
        val messageLayoutPrefsKey = context.resources.getString(R.string.setting_key_message_layout)
        val messageLayoutNormal = context.resources.getString(R.string.message_layout_value_normal)
        val messageLayoutSetting = prefs.getString(messageLayoutPrefsKey, messageLayoutNormal)

        messageLayout = if (messageLayoutSetting == messageLayoutNormal) {
            R.layout.message_item
        } else {
            R.layout.message_item_compact
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (messageLayout == R.layout.message_item) {
            val binding = MessageItemBinding.inflate(layoutInflater, parent, false)
            ViewHolder(binding)
        } else {
            val binding = MessageItemCompactBinding.inflate(layoutInflater, parent, false)
            ViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = currentList[position]
        
        // Clean up the holder before binding new data
        holder.cleanUp()
        when {
            Extras.useMarkdown(message.message) -> {
                holder.message.autoLinkMask = 0
                markwon.setMarkdown(holder.message, message.message.message)
                holder.hideWebView()
            }
            Extras.useHtml(message.message) || Extras.containsHtmlTags(message.message.message) -> {
                if (Extras.hasComplexHtml(message.message)) {
                    holder.showWebView()
                    holder.loadHtmlInWebView(message.message.message)
                } else {
                    holder.hideWebView()
                    holder.message.autoLinkMask = Linkify.WEB_URLS
                    holder.message.text = Html.fromHtml(message.message.message, Html.FROM_HTML_MODE_COMPACT)
                }
            }
            else -> {
                holder.hideWebView()
                holder.message.autoLinkMask = Linkify.WEB_URLS
                holder.message.text = message.message.message
            }
        }
        holder.title.text = message.message.title
        if (message.image != null) {
            val url = Utils.resolveAbsoluteUrl("${settings.url}/", message.image)
            holder.image.load(url, imageLoader) {
                error(R.drawable.ic_alarm)
                placeholder(R.drawable.ic_placeholder)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val timeFormat = prefs.getString(timeFormatPrefsKey, timeFormatRelative)
        holder.setDateTime(message.message.date, timeFormat == timeFormatRelative)
        holder.date.setOnClickListener { holder.switchTimeFormat() }

        holder.delete.setOnClickListener {
            delete.delete(message.message)
        }
        
        // Start with messages collapsed by default
        holder.setCollapsed()
    }

    override fun getItemId(position: Int): Long {
        val currentItem = currentList[position]
        return currentItem.message.id
    }

    // Fix for message not being selectable (https://issuetracker.google.com/issues/37095917)
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.message.isEnabled = false
        holder.message.isEnabled = true
    }

    class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var image: ImageView
        lateinit var message: TextView
        lateinit var title: TextView
        lateinit var date: TextView
        lateinit var delete: ImageButton
        lateinit var expandToggle: ImageButton
        private var webView: WebView? = null
        private var isExpanded = false

        private var relativeTimeFormat = true
        private lateinit var dateTime: OffsetDateTime

        init {
            enableCopyToClipboard()
            if (binding is MessageItemBinding) {
                image = binding.messageImage
                message = binding.messageText
                title = binding.messageTitle
                date = binding.messageDate
                delete = binding.messageDelete
                expandToggle = binding.messageExpandToggle
            } else if (binding is MessageItemCompactBinding) {
                image = binding.messageImage
                message = binding.messageText
                title = binding.messageTitle
                date = binding.messageDate
                delete = binding.messageDelete
                expandToggle = binding.messageExpandToggle
            }
            
            expandToggle.setOnClickListener {
                toggleMessageExpansion()
            }
        }

        fun switchTimeFormat() {
            relativeTimeFormat = !relativeTimeFormat
            updateDate()
        }

        fun setDateTime(dateTime: OffsetDateTime, relativeTimeFormatPreference: Boolean) {
            this.dateTime = dateTime
            relativeTimeFormat = relativeTimeFormatPreference
            updateDate()
        }

        private fun updateDate() {
            val text = if (relativeTimeFormat) {
                // Relative time format
                Utils.dateToRelative(dateTime)
            } else {
                // Absolute time format
                val time = dateTime.toInstant().toEpochMilli()
                val date = Date(time)
                if (DateUtils.isToday(time)) {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
                } else {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
                }
            }

            date.text = text
        }

        private fun enableCopyToClipboard() {
            super.itemView.setOnLongClickListener { view: View ->
                val clipboard = view.context
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip = ClipData.newPlainText("GotifyMessageContent", message.text.toString())
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        view.context,
                        view.context.getString(R.string.message_copied_to_clipboard),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
        }

        fun showWebView() {
            if (webView == null) {
                createWebView()
            }
            message.visibility = View.GONE
            if (isExpanded) {
                webView?.visibility = View.VISIBLE
            }
        }

        fun hideWebView() {
            webView?.visibility = View.GONE
            if (isExpanded) {
                message.visibility = View.VISIBLE
            }
        }

        private fun createWebView() {
            val context = itemView.context
            webView = WebView(context).apply {
                layoutParams = message.layoutParams
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.domStorageEnabled = false
            }
            
            val parent = message.parent as ViewGroup
            val index = parent.indexOfChild(message)
            parent.addView(webView, index + 1)
        }

        fun toggleMessageExpansion() {
            isExpanded = !isExpanded
            if (isExpanded) {
                message.visibility = View.VISIBLE
                webView?.visibility = if (webView != null) View.VISIBLE else View.GONE
                expandToggle.rotation = 90f
            } else {
                message.visibility = View.GONE
                webView?.visibility = View.GONE
                expandToggle.rotation = 0f
            }
        }
        
        fun setCollapsed() {
            isExpanded = false
            message.visibility = View.GONE
            webView?.visibility = View.GONE
            expandToggle.rotation = 0f
        }
        
        fun cleanUp() {
            // Clear the WebView content if it exists and remove it
            webView?.let { webView ->
                webView.loadUrl("about:blank")
                webView.visibility = View.GONE
                val parent = webView.parent as? ViewGroup
                parent?.removeView(webView)
            }
            webView = null
            
            // Reset text view
            message.text = ""
            message.visibility = View.GONE
            
            // Reset state
            isExpanded = false
            expandToggle.rotation = 0f
        }

        fun loadHtmlInWebView(htmlContent: String) {
            val styledHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { 
                            font-family: sans-serif; 
                            margin: 0; 
                            padding: 8px; 
                            color: #333;
                            background: transparent;
                            line-height: 1.5;
                            font-size: 14px;
                        }
                        /* Elementos plegables nativos */
                        details {
                            border: 1px solid #ddd;
                            border-radius: 6px;
                            padding: 0;
                            margin: 8px 0;
                            background: #fafafa;
                            overflow: hidden;
                        }
                        summary {
                            cursor: pointer;
                            font-weight: bold;
                            padding: 12px;
                            background: #f0f0f0;
                            margin: 0;
                            border-bottom: 1px solid #ddd;
                            user-select: none;
                            list-style: none;
                        }
                        summary:hover {
                            background: #e8e8e8;
                        }
                        summary::-webkit-details-marker {
                            display: none;
                        }
                        summary::before {
                            content: '▶';
                            margin-right: 8px;
                            transition: transform 0.2s;
                        }
                        details[open] summary::before {
                            transform: rotate(90deg);
                        }
                        details > *:not(summary) {
                            padding: 12px;
                        }
                        
                        /* Otros elementos */
                        .collapsible {
                            border: 1px solid #ddd;
                            margin: 8px 0;
                            border-radius: 4px;
                        }
                        .collapsible-header {
                            background: #f5f5f5;
                            padding: 12px;
                            cursor: pointer;
                            font-weight: bold;
                            border-bottom: 1px solid #ddd;
                        }
                        .collapsible-content {
                            padding: 12px;
                        }
                        
                        /* Estilos generales */
                        a { color: #1976d2; text-decoration: none; }
                        a:hover { text-decoration: underline; }
                        h1, h2, h3, h4, h5, h6 { margin: 12px 0 8px 0; color: #2c3e50; }
                        h1 { font-size: 1.8em; }
                        h2 { font-size: 1.5em; }
                        h3 { font-size: 1.3em; }
                        h4 { font-size: 1.1em; }
                        h5 { font-size: 1em; }
                        h6 { font-size: 0.9em; }
                        p { margin: 8px 0; }
                        ul, ol { margin: 8px 0; padding-left: 24px; }
                        li { margin: 4px 0; }
                        blockquote { 
                            border-left: 4px solid #ddd;
                            padding-left: 12px;
                            margin: 8px 0;
                            font-style: italic;
                            background: #f9f9f9;
                        }
                        code {
                            background: #f4f4f4;
                            padding: 2px 4px;
                            border-radius: 3px;
                            font-family: monospace;
                        }
                        pre {
                            background: #f4f4f4;
                            padding: 12px;
                            border-radius: 4px;
                            overflow-x: auto;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%;
                            margin: 8px 0;
                        }
                        th, td {
                            border: 1px solid #ddd;
                            padding: 8px;
                            text-align: left;
                        }
                        th {
                            background: #f0f0f0;
                            font-weight: bold;
                        }
                        hr {
                            border: none;
                            border-top: 1px solid #ddd;
                            margin: 16px 0;
                        }
                        .highlight {
                            background: yellow;
                            padding: 2px 4px;
                        }
                        .text-center { text-align: center; }
                        .text-right { text-align: right; }
                        .bold { font-weight: bold; }
                        .italic { font-style: italic; }
                        .underline { text-decoration: underline; }
                    </style>
                </head>
                <body>
                    $htmlContent
                </body>
                </html>
            """.trimIndent()
            
            webView?.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MessageWithImage>() {
        override fun areItemsTheSame(
            oldItem: MessageWithImage,
            newItem: MessageWithImage
        ): Boolean {
            return oldItem.message.id == newItem.message.id
        }

        override fun areContentsTheSame(
            oldItem: MessageWithImage,
            newItem: MessageWithImage
        ): Boolean {
            return oldItem == newItem
        }
    }

    fun interface Delete {
        fun delete(message: Message)
    }
}
