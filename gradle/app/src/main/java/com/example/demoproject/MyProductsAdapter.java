package com.example.demoproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.demoproject.model.Product;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MyProductsAdapter extends BaseAdapter {
    private Context context;
    private List<Product> productList;
    private LayoutInflater inflater;

    public MyProductsAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_myproduct, parent, false);
            holder = new ViewHolder();
            holder.productName = convertView.findViewById(R.id.list_item_myprdouct_fragment_productName);
            holder.productAmount = convertView.findViewById(R.id.list_item_myprdouct_fragment_productAmount);
            holder.productImage = convertView.findViewById(R.id.list_item_myprdouct_fragment_productImage);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Load image using Picasso
        Picasso.get().load(productList.get(position).getImage()).into(holder.productImage);

        final Product product = (Product) getItem(position);
        holder.productName.setText(product.getName());
        holder.productAmount.setText(product.getAmount());

        return convertView;
    }

    static class ViewHolder {
        TextView productName;
        TextView productAmount;
        ImageView productImage;
    }
}