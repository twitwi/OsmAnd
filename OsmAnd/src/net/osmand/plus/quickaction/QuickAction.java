package net.osmand.plus.quickaction;


import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickAction {

    public interface QuickActionSelectionListener {

        void onActionSelected(@NonNull QuickActionButtonState buttonState, @NonNull QuickAction action);
    }
    private static int SEQ;

    protected long id;
    private String name;
    private Map<String, String> params;
    private QuickActionType actionType;

    protected QuickAction() {
        this(MapButtonsHelper.TYPE_ADD_ITEMS);
    }

    public QuickAction(QuickActionType type) {
        this.id = System.currentTimeMillis() + (SEQ++);
        this.actionType = type;
    }

    public QuickAction(QuickAction quickAction) {
		this.actionType = quickAction.actionType;
        this.id = quickAction.id;
        this.name = quickAction.name;
        this.params = quickAction.params;
    }

    public int getNameRes() {
    	return actionType == null ? 0 : actionType.getNameRes();
    }

	public int getActionNameRes() {
		return actionType == null ? 0 : actionType.getActionNameRes();
	}

    public int getIconRes() {
		return actionType == null ? 0 : actionType.getIconRes();
    }

    public int getIconRes(Context context) {
		return actionType == null ? 0 : actionType.getIconRes();
    }

    public long getId() {
        return id;
    }

	public int getType() {
		return actionType.getId();
	}

	public void setActionType(QuickActionType actionType) {
		this.actionType = actionType;
	}

    public boolean isActionEditable() {
        return actionType != null && actionType.isActionEditable();
    }

    public boolean isActionEnable(OsmandApplication app) {
        return true;
    }

	public String getName(@NonNull Context context) {
		if (Algorithms.isEmpty(name) || !isActionEditable()) {
			return getDefaultName(context);
		} else {
			return name;
		}
	}

	public String getRawName() {
		return name;
	}

	@NonNull
	private String getDefaultName(@NonNull Context context) {
		return getNameRes() != 0 ? context.getString(getNameRes()) : "";
	}

	@NonNull
	public Map<String, String> getParams() {
        if (params == null) {
        	params = new HashMap<>();
		}
        return params;
    }

    public void setName(String name) {
        this.name = name;
    }

	public void setId(long id) {
		this.id = id;
	}

	public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public boolean isActionWithSlash(@NonNull OsmandApplication app){
        return false;
    }

    public String getActionText(@NonNull OsmandApplication app){
        return getName(app);
    }

	public QuickActionType getActionType() {
		return actionType;
	}

	public void setAutoGeneratedTitle(EditText title) {
    }

	@NonNull
	public LatLon getMapLocation(@NonNull Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
		int centerPixX = tb.getCenterPixelX();
		int centerPixY = tb.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, centerPixX, centerPixY);
	}

	public boolean onKeyDown(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		return true;
	}

	public boolean onKeyLongPress(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		return true;
	}

	public boolean onKeyUp(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		execute(mapActivity);
		return true;
	}

	public boolean onKeyMultiple(@NonNull MapActivity mapActivity, int keyCode, int count, KeyEvent event) {
		return true;
	}

    public void execute(@NonNull MapActivity mapActivity) {
	}

	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
	}

    public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
    	return true;
    }

    public boolean hasInstanceInList(List<QuickAction> active) {

		for (QuickAction action : active) {
			if (action.getType() == getType()) return true;
		}

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (o instanceof QuickAction) {

            QuickAction action = (QuickAction) o;

            if (getType() != action.getType()) return false;
	        return id == action.id;

        } else return false;
    }

    @Override
    public int hashCode() {
        int result = getType();
        result = 31 * result + (int) (id ^ (id >>> 32));
        return result;
    }

    public boolean hasCustomName(Context context) {
        return !getName(context).equals(getDefaultName(context));
    }
}
