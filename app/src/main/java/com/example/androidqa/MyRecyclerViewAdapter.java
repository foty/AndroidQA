package com.example.androidqa;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Create by lxx
 * Date : 2021/12/31 10:58
 * Use by
 */
class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyViewHolder> {
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}


class MyViewHolder extends RecyclerView.ViewHolder{

    public MyViewHolder(@NonNull View itemView) {
        super(itemView);
    }
}