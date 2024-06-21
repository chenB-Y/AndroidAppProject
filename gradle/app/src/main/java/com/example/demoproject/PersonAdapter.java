// PersonAdapter.java
package com.example.demoproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.demoproject.model.Product;

import java.util.List;

public class PersonAdapter extends BaseAdapter {
    private Context context;
    private List<Product> personList;
    private LayoutInflater inflater;

    public PersonAdapter(Context context, List<Product> personList) {
        this.context = context;
        this.personList = personList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return personList.size();
    }

    @Override
    public Object getItem(int position) {
        return personList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_product, parent, false);
            holder = new ViewHolder();
            holder.nameTextView = convertView.findViewById(R.id.list_item_myprdouct_fragment_productName);
            holder.ageTextView = convertView.findViewById(R.id.list_item_myprdouct_fragment_productAmount);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product person = personList.get(position);
        holder.nameTextView.setText(person.getName());
        holder.ageTextView.setText(person.getAmount());
        holder.ageTextView.setText(person.getImage());

        return convertView;
    }

    static class ViewHolder {
        TextView nameTextView;
        TextView ageTextView;
    }
}