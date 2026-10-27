package com.vpmedia.sdkvbot.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectMemberActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TO_CALL = "extra_to_call"
        const val EXTRA_SELECTED_MEMBER = "extra_selected_member"

        fun newIntent(context: Context, toCall: Boolean): Intent {
            return Intent(context, SelectMemberActivity::class.java)
                .putExtra(EXTRA_TO_CALL, toCall)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_member)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toCall = intent.getBooleanExtra(EXTRA_TO_CALL, false)
        title = if (toCall) "Chọn để gọi" else "Chọn để kết nối"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val connectedName = getSDKMember(this)?.callName ?: ""
        val dataTable = if (toCall) {
            TestAccount.filter { it.callName != connectedName }
        } else {
            TestAccount.filter { it.type == "android" && it.callName != connectedName }
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerMembers)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = MemberAdapter(dataTable) { member ->
            val data = Intent().putExtra(EXTRA_SELECTED_MEMBER, member.toJson())
                .putExtra(EXTRA_TO_CALL, toCall)
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

private class MemberAdapter(
    private val items: List<SDKMember>,
    private val onClick: (SDKMember) -> Unit
) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameLabel: TextView = view.findViewById(R.id.tvMemberName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nameLabel.text = item.callName
        val color = if (item.typeTest == "xanhsm") Color.parseColor("#009688") else Color.parseColor("#4CAF50")
        holder.itemView.background.mutate().setTint(color)
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
