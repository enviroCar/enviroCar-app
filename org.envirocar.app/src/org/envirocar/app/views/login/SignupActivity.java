package org.envirocar.app.views.login;

import android.os.Bundle;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * @author dewall
 */
public class SignupActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(SignupActivity.class);

    // Inject Dependencies
    @Inject
    protected UserHandler userHandler;
    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected AgreementManager agreementManager;

    // Injected Views

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // inject the views
        ButterKnife.bind(this);
    }

//    @OnClick(R.id.activity_signin_register_button)
//    protected void onSwitchToRegister(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
//            Intent intent = new Intent(this, SigninActivity.class);
//            intent.putExtra("from", "login");
//            startActivity(intent);
//        } else {
//            Intent intent = new Intent(this, SigninActivity.class);
//            intent.putExtra("from", "login");
//            startActivity(intent);
//        }
//    }

}
