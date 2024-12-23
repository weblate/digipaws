package nethical.digipaws.blockers

import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class KeywordBlocker : BaseBlocker() {
    companion object {
        val URL_BAR_ID_LIST = mapOf(

            "com.android.chrome" to BrowserUrlBarInfo(
                displayUrlBarId = "url_bar",
                browserSugggestionBoxId = "omnibox_suggestions_dropdown"
            ),
            "com.brave.browser" to BrowserUrlBarInfo(
                displayUrlBarId = "url_bar",
                browserSugggestionBoxId = "omnibox_suggestions_dropdown"
            ),
            // Todo; Fix firefox redirector not working because fails to access the edittext
            "org.mozilla.firefox" to BrowserUrlBarInfo(
                displayUrlBarId = "mozac_browser_toolbar_url_view",
                browserSugggestionBoxId = "sfcnt",
            ),
            "com.opera.browser" to BrowserUrlBarInfo(
                displayUrlBarId = "url_field",
                browserSugggestionBoxId = "right_state_button",
                isSuggestionEqualToGo = true
            ),
        )

    }
    lateinit var blockedKeyword: HashSet<String>

    lateinit var redirectUrl: String
    var isSearchAllTextFields = false

    var recursionResultNodes: MutableList<AccessibilityNodeInfo> = mutableListOf()
    private fun containsBlockedKeyword(url: String): String? {
        // Split text by whitespace to get individual words and check each word
        val keywords = parseTextForKeywords(url)
        keywords.forEach { word ->
            if (blockedKeyword.contains(word.lowercase())) {
                return word
            }
        }
        return null
    }

    private fun parseTextForKeywords(input: String): Set<String> {
        // Basic word extraction for any text
        fun extractWords(text: String): Set<String> {
            return text.split(Regex("[^a-zA-Z0-9]+"))
                .filter { it.isNotEmpty() }
                .map { it.toLowerCase() }
                .toSet()
        }

        // Simple URL pattern
        val urlPattern = "([\\w-]+\\.)+[\\w-]+(/[^?#]*)?\\??([^#]*)?"

        // First check if it matches URL pattern
        val regex = Regex(urlPattern)
        val words = mutableSetOf<String>()

        if (regex.find(input) != null) {
            // Handle as URL
            words.addAll(extractWords(input))

            // Extract query parameters specifically
            regex.find(input)?.groups?.get(3)?.value?.let { queryParams ->
                queryParams.split('&').forEach { param ->
                    if ('=' in param) {
                        val (key, value) = param.split('=', limit = 2)
                        words.addAll(extractWords(key))
                        words.addAll(extractWords(value))
                    }
                }
            }
        } else {
            // Handle as plain text
            words.addAll(extractWords(input))
        }

        return words
    }

    fun checkIfUserGettingFreaky(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent
    ): KeywordBlockerResult {
        rootNode ?: return KeywordBlockerResult()
        val displayUrlTextNode: AccessibilityNodeInfo?

        var detectedAdultKeyword: String? = null

        if (isSearchAllTextFields) {

            recursionResultNodes.clear()
            findNodesByClassName(rootNode, "android.widget.TextView", false)

            try {
                recursionResultNodes.forEach { node ->
                    val word = containsBlockedKeyword(node.text.toString())
                    if (word != null) {
                        detectedAdultKeyword = word
                        return@forEach
                    }
                }
            } catch (e: Exception) {
                Log.d("Keyword Blocker 111", e.toString())
            }
        }
        // Check if the package name exists in the map
        val urlBarInfo = URL_BAR_ID_LIST[event.packageName]
        if (urlBarInfo == null && detectedAdultKeyword != null) {
            // App is not a supported browser and adult word was found so hence press home
            return KeywordBlockerResult(true, detectedAdultKeyword)
        }

        if (urlBarInfo == null) return KeywordBlockerResult()
        val idPrefixPart = event.packageName.toString() + ":id/"
        displayUrlTextNode =
            ViewBlocker.findElementById(rootNode, idPrefixPart + urlBarInfo.displayUrlBarId)



        if (detectedAdultKeyword == null) {

            detectedAdultKeyword = searchKeywordsInWebViewTitle(rootNode) ?: containsBlockedKeyword(
                displayUrlTextNode?.text.toString()
            ) ?: return KeywordBlockerResult()

        }

        displayUrlTextNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Thread.sleep(200)

        Log.d("edit", idPrefixPart + urlBarInfo.editUrlBarId)


        val editUrlBarId = urlBarInfo.editUrlBarId ?: urlBarInfo.displayUrlBarId
        val editUrlBar = ViewBlocker.findElementById(rootNode, idPrefixPart + editUrlBarId)
            ?: return KeywordBlockerResult(false, detectedAdultKeyword)

        editUrlBar.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, redirectUrl
            )
        })
        Thread.sleep(300)
        val goBtnNode =
            ViewBlocker.findElementById(rootNode, idPrefixPart + urlBarInfo.browserSugggestionBoxId)
                ?: return KeywordBlockerResult(resultDetectWord = detectedAdultKeyword)
        if (urlBarInfo.isSuggestionEqualToGo) {
            goBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            goBtnNode.getChild(urlBarInfo.suggestionBoxIndexOfGoBtn)
                .performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        return KeywordBlockerResult(resultDetectWord = detectedAdultKeyword)
    }

    private fun searchKeywordsInWebViewTitle(rootNode: AccessibilityNodeInfo): String? {

        recursionResultNodes.clear()
        try {
            findNodesByClassName(rootNode, "android.webkit.WebView")
        } catch (e: Exception) {
            Log.d("error", e.toString())
            return null
        }
        val webView = recursionResultNodes.getOrNull(0) ?: return null
        Log.d("Keyword Blocker", "Webview found")
        val resultWord = webView.text

        Log.d("Keyword Blocker", "Webview title $resultWord")

        return containsBlockedKeyword(resultWord.toString())

    }


    private fun findNodesByClassName(
        node: AccessibilityNodeInfo?,
        targetClassName: String,
        returnOnFirstResult: Boolean = true
    ) {
        node ?: return

        if (node.className == targetClassName) {
            recursionResultNodes.add(node)
        }

        for (i in 0 until node.childCount) {
            findNodesByClassName(node.getChild(i), targetClassName)
        }
        if (returnOnFirstResult && recursionResultNodes.isNotEmpty()) return
        return
    }


    data class BrowserUrlBarInfo(
        val displayUrlBarId: String,
        val editUrlBarId: String? = null,
        val browserSugggestionBoxId: String,
        val suggestionBoxIndexOfGoBtn: Int = 0,
        val isSuggestionEqualToGo: Boolean = false
    )

    data class KeywordBlockerResult(
        val isHomePressRequested: Boolean = false,
        val resultDetectWord: String? = null
    )
}