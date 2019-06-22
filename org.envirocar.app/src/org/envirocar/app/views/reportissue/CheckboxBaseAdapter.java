package org.envirocar.app.views.reportissue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.envirocar.app.R;

import java.util.List;

public class CheckboxBaseAdapter extends BaseAdapter {
    private List<CheckBoxItem> checkBoxItems = null;

    private Context ctx = null;

    public CheckboxBaseAdapter(Context ctx, List<CheckBoxItem> checkBoxItems) {
        this.ctx = ctx;
        this.checkBoxItems = checkBoxItems;
    }

    @Override
    public int getCount() {
        int ret = 0;
        if(checkBoxItems!=null)
        {
            ret = checkBoxItems.size();
        }
        return ret;
    }

    @Override
    public Object getItem(int itemIndex) {
        Object ret = null;
        if(checkBoxItems!=null) {
            ret = checkBoxItems.get(itemIndex);
        }
        return ret;
    }

    @Override
    public long getItemId(int itemIndex) {
        return itemIndex;
    }

    /**
     * Sets all the fields of the ViewHolder and also sets the on click listener for the Checkbox
     * @param itemIndex
     * @param convertView
     * @param viewGroup
     * @return
     */
    @Override
    public View getView(int itemIndex, View convertView, ViewGroup viewGroup) {

        CheckboxViewHolder viewHolder = null;

        if (convertView != null) {
            viewHolder = (CheckboxViewHolder) convertView.getTag();
        } else {
            convertView = View.inflate(ctx, R.layout.report_issue_checkbox_item, null);

            CheckBox listItemCheckbox = convertView.findViewById(R.id.report_issue_checkbox_item);

            TextView listItemText = convertView.findViewById(R.id.report_issue_check_text);

            viewHolder = new CheckboxViewHolder(convertView);

            viewHolder.setItemCheckbox(listItemCheckbox);

            viewHolder.setItemTextView(listItemText);

            convertView.setTag(viewHolder);

            viewHolder.getItemCheckbox().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox temp = (CheckBox) v;
                    CheckBoxItem t = (CheckBoxItem) temp.getTag();
                    t.setChecked(temp.isChecked());

                }
            });
        }

        CheckBoxItem checkBoxItem = checkBoxItems.get(itemIndex);
        viewHolder.getItemCheckbox().setChecked(checkBoxItem.isChecked());
        viewHolder.getItemTextView().setText(checkBoxItem.getItemText());
        viewHolder.getItemCheckbox().setTag(checkBoxItem);
        return convertView;

    }
}
