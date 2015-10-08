package org.envirocar.app.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.util.Util;

/**
 * Help page
 *
 * @author jakob
 */
public class HelpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.help, container, false);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //TODO: this could possibly be used to make links in the text able to be opened in the browser
        //gave me an error at first try, though...
        //t2.setMovementMethod(LinkMovementMethod.getInstance());

//		SpannableString text = new SpannableString(getActivity().getText(R.string.help_view_track_text_1));
//		
//		Locale locale = Locale.getDefault();
//		
//		//add symbol in text (position differs depending on language)
//		//TODO you will probably also have to change the data_privacy image here (text on the image is in german right now) 
//		if(locale.equals(Locale.GERMANY)){
//			text.setSpan(is, 103, 117, 0);
//		}else if(locale.equals(Locale.UK) || locale.equals(Locale.US)){
//			text.setSpan(is, 103, 117, 0);
//			
//		}


        TextView versionTextview = (TextView) getActivity().findViewById(R.id.textView22);

        CharSequence versionString = getActivity().getText(R.string.help_text_6_3);

        versionString = versionString + " " + Util.getVersionString(getActivity());

        versionTextview.setText(versionString);

    }
}
