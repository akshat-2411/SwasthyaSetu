package com.example.swasthyasetu.view

import com.example.swasthyasetu.databinding.ItemChatAiBinding
import com.example.swasthyasetu.databinding.ItemChatUserBinding
import com.example.swasthyasetu.model.ChatMessage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
        } else {
            AiViewHolder(ItemChatAiBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserViewHolder -> holder.binding.tvUserMessage.text = msg.text
            is AiViewHolder -> holder.binding.tvAiMessage.text = msg.text
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    inner class UserViewHolder(val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class AiViewHolder(val binding: ItemChatAiBinding) :
        RecyclerView.ViewHolder(binding.root)
}
