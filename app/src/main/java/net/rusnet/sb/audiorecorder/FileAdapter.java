package net.rusnet.sb.audiorecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private List<File> mFiles;

    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public void setFiles(List<File> files) {
        mFiles = files;
    }

    public FileAdapter(List<File> files) {
        mFiles = files;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View fileView = inflater.inflate(R.layout.item_file, parent, false);

        ViewHolder holder = new ViewHolder(fileView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = mFiles.get(position);

        TextView fileTextView = holder.mFileTextView;
        fileTextView.setText(file.getName());
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mFileTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mFileTextView = itemView.findViewById(R.id.text_view_file_name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(mFiles.get(getAdapterPosition()));
                    }
                }
            });
        }

    }
}
