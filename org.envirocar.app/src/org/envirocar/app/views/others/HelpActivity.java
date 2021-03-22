package org.envirocar.app.views.others;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.envirocar.app.R;

import butterknife.ButterKnife;
import butterknife.BindView;
/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class HelpActivity extends AppCompatActivity {
    @BindView(R.id.activity_help_layout_general_toolbar)
    protected Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_layout_general);

        // Inject views
        ButterKnife.bind(this);

        TextView textView = findViewById(R.id.aboutText);
        String text = "This app is part of the Citizen Science platform enviroCar (www.enviroCar.org). You can use it to collect and analyze test drives and provide the measured data as Open Data.\n\nenviroCar is currently in a beta phase. As a result, the software’s stability is not yet comparable to standards associated with mature software.\n\nWe welcome your feedback (enviroCar@52north.org)! Your advice and suggestions help us to improve the software.\n\n";
        SpannableString ss = new SpannableString(text);
        ClickableSpan clickableSpan1 = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(HelpActivity.this,WebsiteView.class));
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
            }
        };
        ss.setSpan(clickableSpan1, text.indexOf('(')+1, text.indexOf(')'), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        // Set Actionbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Help");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        super.onOptionsItemSelected(item);
        return false;
    }
}
