package nethical.digipaws.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.R
import nethical.digipaws.databinding.ActivityManageKeywordsBinding
import nethical.digipaws.databinding.DialogAddKeywordBinding


class ManageKeywordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageKeywordsBinding
    lateinit var savedKeywordsList: ArrayList<String>
    private lateinit var keywordAdapter: KeywordAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityManageKeywordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        savedKeywordsList = intent.getStringArrayListExtra("PRE_SAVED_KEYWORDS") ?: arrayListOf()

        keywordAdapter = KeywordAdapter(savedKeywordsList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = keywordAdapter

        binding.confirmSelectionKeywords.setOnClickListener {
            val resultIntent = intent.apply {
                putStringArrayListExtra("SELECTED_KEYWORDS", savedKeywordsList)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        binding.btnAddKeyword.setOnClickListener { makeAddKeywordDialog() }

    }

    private fun makeAddKeywordDialog() {
        val dialogBinding = DialogAddKeywordBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_a_new_keyword))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                val keyword = dialogBinding.keywordInput.text.toString().trim()
                if (keyword.isNotEmpty()) {
                    savedKeywordsList.add(keyword)
                    keywordAdapter.notifyItemInserted(savedKeywordsList.size - 1)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    inner class KeywordAdapter(private val keywords: ArrayList<String>) :
        RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder>() {

        inner class KeywordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val keywordTextView: TextView = itemView.findViewById(R.id.keyword_txt)
            val removeBtn: Button = itemView.findViewById(R.id.btn_remove_keyword)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.keyword_item, parent, false)
            return KeywordViewHolder(view)
        }

        override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
            holder.keywordTextView.text = keywords[position]
            holder.removeBtn.setOnClickListener {
                savedKeywordsList.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        override fun getItemCount(): Int = keywords.size
    }
}
