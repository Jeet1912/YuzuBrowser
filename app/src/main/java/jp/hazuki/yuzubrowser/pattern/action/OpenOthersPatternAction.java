package jp.hazuki.yuzubrowser.pattern.action;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.net.URISyntaxException;

import jp.hazuki.yuzubrowser.R;
import jp.hazuki.yuzubrowser.pattern.PatternAction;
import jp.hazuki.yuzubrowser.tab.MainTabData;
import jp.hazuki.yuzubrowser.utils.ErrorReport;
import jp.hazuki.yuzubrowser.utils.PackageUtils;

public class OpenOthersPatternAction extends PatternAction {
    private static final String FIELD_TYPE = "0";
    private static final String FIELD_INTENT = "1";
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_APP_LIST = 1;
    public static final int TYPE_APP_CHOOSER = 2;
    private int mType;
    private Intent mIntent;

    public OpenOthersPatternAction(Intent intent) {
        mType = TYPE_NORMAL;
        mIntent = intent;
    }

    public OpenOthersPatternAction(int type) {
        mType = type;
    }

    public OpenOthersPatternAction(JsonParser parser) throws IOException {
        if (parser.nextToken() != JsonToken.START_OBJECT) return;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.getCurrentToken() != JsonToken.FIELD_NAME) return;
            if (FIELD_TYPE.equals(parser.getCurrentName())) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                mType = parser.getIntValue();
                continue;
            }
            if (FIELD_INTENT.equals(parser.getCurrentName())) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) return;
                try {
                    mIntent = Intent.parseUri(parser.getText(), 0);
                } catch (URISyntaxException e) {
                    ErrorReport.printAndWriteLog(e);
                }
                continue;
            }
            parser.skipChildren();
        }
    }

    @Override
    public int getTypeId() {
        return OPEN_OTHERS;
    }

    @Override
    public boolean write(JsonGenerator generator) throws IOException {
        generator.writeNumber(OPEN_OTHERS);
        generator.writeStartObject();
        generator.writeNumberField(FIELD_TYPE, mType);
        if (mIntent != null)
            generator.writeStringField(FIELD_INTENT, mIntent.toUri(0));
        generator.writeEndObject();
        return true;
    }

    @Override
    public String getTitle(Context context) {
        switch (mType) {
            case TYPE_NORMAL: {
                String pre = context.getString(R.string.pattern_open_others);
                try {
                    PackageManager pm = context.getPackageManager();
                    return pre + " : " + pm.getActivityInfo(mIntent.getComponent(), 0).loadLabel(pm).toString();
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
                return pre;
            }
            case TYPE_APP_LIST:
                return context.getString(R.string.pattern_open_app_list);
            case TYPE_APP_CHOOSER:
                return context.getString(R.string.pattern_open_app_chooser);
            default:
                throw new IllegalStateException();
        }
    }

    public int getOpenType() {
        return mType;
    }

    public Intent getIntent() {
        return mIntent;
    }

    @Override
    public boolean run(Context context, MainTabData tab, String url) {
        Intent intent;
        switch (mType) {
            case TYPE_NORMAL:
                intent = new Intent(mIntent);
                intent.setData(Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case TYPE_APP_LIST:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                break;
            case TYPE_APP_CHOOSER:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent = PackageUtils.createChooser(context, url, context.getText(R.string.open));
                break;
            default:
                throw new IllegalStateException();
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }
}
