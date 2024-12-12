package nethical.digipaws.blockers

import android.view.accessibility.AccessibilityNodeInfo

class KeywordBlocker : BaseBlocker() {
    lateinit var blockedKeyword: HashSet<String>

    private fun containsBlockedKeyword(text: String): String? {
        // Split text by whitespace to get individual words and check each word
        text.split("\\s+".toRegex()).forEach { word ->
            if (blockedKeyword.contains(word.lowercase())) {
                return word
            }
        }
        return null
    }

    private fun checkIfNodeEditText(node: AccessibilityNodeInfo?): Boolean {
        return node?.className == "android.widget.EditText"
    }


    private fun checkIfHasAdultKeyword(node: AccessibilityNodeInfo?): String? {
        node ?: return null

        val nodeText = node.text ?: return null // If there's no text, skip this node
        val detectedAdultKeyword = containsBlockedKeyword(nodeText.toString())
        if (detectedAdultKeyword != null) {
            return detectedAdultKeyword
        }
        return null
    }

    fun checkIfUserGettingFreaky(node: AccessibilityNodeInfo?): String? {
        node ?: return null

        if (checkIfNodeEditText(node)) {
            val word = checkIfHasAdultKeyword(node)
            if (word != null) return word
        }

        for (i in 0 until node.childCount) {
            val result = checkIfUserGettingFreaky(node.getChild(i))
            if (result != null) return result // Return the first keyword found
        }
        return null
    }


}