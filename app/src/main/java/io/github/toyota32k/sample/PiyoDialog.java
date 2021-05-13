package io.github.toyota32k.sample;

import android.os.Bundle;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.toyota32k.R;
import io.github.toyota32k.dialog.IUtDialogHost;
import io.github.toyota32k.dialog.IUtDialogResultReceptor;
import io.github.toyota32k.dialog.UtDialog;
import io.github.toyota32k.dialog.UtDialogHostManager;
import io.github.toyota32k.utils.UtLog;

public class PiyoDialog extends UtDialog implements View.OnClickListener, IUtDialogHost {
    private final UtLog logger = new UtLog("Piyo");
    private final UtDialogHostManager dialogHostManager = new UtDialogHostManager();
    private final UtDialogHostManager.ReceptorImpl receptor = dialogHostManager.register("piyo.fuga", (s)->{
        logger.info("completed Fuga");
        return null;
    });

    @Override
    public void preCreateBodyView() {
        setTitle("Piyoダイアログ");
        setLeftButton(BuiltInButtonType.CANCEL);
        setRightButton(BuiltInButtonType.DONE);
        setGravityOption(GravityOption.LEFT_TOP);
        setHeightOption(HeightOption.FULL);
        setWidthOption(WidthOption.COMPACT);
        setCancellable(false);
    }

    @NotNull
    @Override
    protected View createBodyView(@Nullable Bundle savedInstanceState, @NotNull IViewInflater inflater) {

        View view = inflater.inflate(R.layout.sample_piyo_dialog);
        view.findViewById(R.id.first_button).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        receptor.showDialog(this, (r)->{ return new FugaDialog(); });
    }

    @Nullable
    @Override
    public IUtDialogResultReceptor queryDialogResultReceptor(@NotNull String tag) {
        return dialogHostManager.queryDialogResultReceptor(tag);
    }
}
