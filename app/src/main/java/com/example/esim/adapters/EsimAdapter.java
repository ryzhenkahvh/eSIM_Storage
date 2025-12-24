package com.example.esim.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.esim.EsimProfile;
import com.example.esim.R;

import java.util.List;

public class EsimAdapter extends RecyclerView.Adapter<EsimAdapter.ViewHolder> {

    private List<EsimProfile> profiles;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EsimProfile profile);
    }

    public EsimAdapter(List<EsimProfile> profiles, OnItemClickListener listener) {
        this.profiles = profiles;
        this.listener = listener;
    }

    public void updateData(List<EsimProfile> newProfiles) {
        this.profiles = newProfiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_esim_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EsimProfile profile = profiles.get(position);
        holder.tvName.setText(profile.name != null && !profile.name.isEmpty() ? profile.name : "eSIM Profile #" + profile.id);
        // Показываем часть кода активации
        holder.tvProvider.setText("LPA:1..." + (profile.activationCode.length() > 10 ? profile.activationCode.substring(0, 10) : profile.activationCode));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(profile));
    }

    @Override
    public int getItemCount() {
        return profiles != null ? profiles.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvProvider;

        public ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_profile_name);
            tvProvider = view.findViewById(R.id.tv_provider);
        }
    }
}