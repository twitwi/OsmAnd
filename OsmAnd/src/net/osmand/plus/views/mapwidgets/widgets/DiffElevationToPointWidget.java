package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_INTERMEDIATE;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetstates.DiffElevationToPointWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState.TimeToNavigationPointState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.util.Algorithms;

import java.util.concurrent.TimeUnit;

public class DiffElevationToPointWidget extends SimpleWidget {

	private static final long UPDATE_INTERVAL_SECONDS = 30;

	private final RoutingHelper routingHelper;
	private final DiffElevationToPointWidgetState widgetState;
	//private final OsmandPreference<Boolean> arrivalTimeOtherwiseTimeToGoPref;
	private final OsmandPreference<DiffElevationToPointWidgetState.DiffElevationType> typePreference;

	//private boolean cachedArrivalTimeOtherwiseTimeToGo;
	private DiffElevationToPointWidgetState.DiffElevationType cachedTypePreference;
	private int cachedLeftSeconds;

	public DiffElevationToPointWidget(@NonNull MapActivity mapActivity, @NonNull DiffElevationToPointWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, getWidgetType(widgetState.isIntermediate()), customId, widgetsPanel);
		this.widgetState = widgetState;
		this.routingHelper = app.getRoutingHelper();
		//this.arrivalTimeOtherwiseTimeToGoPref = widgetState.getPreference();
		//this.cachedArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.get();
		this.typePreference = widgetState.getPreference();
		this.cachedTypePreference = typePreference.get();

		setText(null, null);
		updateIcons();
		updateContentTitle();
		setOnClickListener(getOnClickListener());
		updateWidgetName();
	}

	private static WidgetType getWidgetType(boolean isIntermediate) {
		return isIntermediate ? TIME_TO_INTERMEDIATE : TIME_TO_DESTINATION;
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			widgetState.changeToNextState();
			updateInfo(null);
			mapActivity.refreshMap();
			updateWidgetName();
		};
	}

	public boolean isIntermediate() {
		return widgetState.isIntermediate();
	}

	/*
	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return arrivalTimeOtherwiseTimeToGoPref;
	}
	 */

	@Nullable
	@Override
	public WidgetState getWidgetState() {
		return widgetState;
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int leftSeconds = 0;

		boolean modeUpdated = typePreference.get() != cachedTypePreference;
		if (modeUpdated) {
			cachedTypePreference = typePreference.get();
			updateIcons();
			updateContentTitle();
		}
		/*
		boolean timeModeUpdated = arrivalTimeOtherwiseTimeToGoPref.get() != cachedArrivalTimeOtherwiseTimeToGo;
		if (timeModeUpdated) {
			cachedArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.get();
			updateIcons();
			updateContentTitle();
		}
		*/

		if (routingHelper.isRouteCalculated()) {
			// maybe leftDiffEle......
			leftSeconds = widgetState.isIntermediate() ? routingHelper.getLeftTimeNextIntermediate() : routingHelper.getLeftTime();
			boolean updateIntervalPassed = Math.abs(leftSeconds - cachedLeftSeconds) > UPDATE_INTERVAL_SECONDS;
			if (leftSeconds != 0 && (updateIntervalPassed || modeUpdated)) {
				cachedLeftSeconds = leftSeconds;
				updateDiffElevationToGo(routingHelper.getLeftDiffElevation());
			}
		}

		if (leftSeconds == 0 && cachedLeftSeconds != 0) {
			cachedLeftSeconds = 0;
			setText(null, null);
		}
	}

	private void updateIcons() {
		DiffElevationToPointWidgetState.DiffElevationType state = getCurrentState();
		setIcons(state.dayIconId, state.nightIconId);
	}

	private void updateContentTitle() {
		String title = getCurrentState().getTitle(app);
		setContentTitle(title);
	}

	private void updateDiffElevationToGo(RouteCalculationResult.DiffElevation diffElevation) {
		String formattedAltUp = OsmAndFormatter.getFormattedAlt(diffElevation.up, app);
		String formattedAltDown = OsmAndFormatter.getFormattedAlt(diffElevation.down, app);
		setText(formattedAltUp + "+", formattedAltDown + "-");
	}

	private void updateArrivalTime(int leftSeconds) {
		long arrivalTime = System.currentTimeMillis() + leftSeconds * 1000L;
		setTimeText(arrivalTime);
	}

	private void updateTimeToGo(int leftSeconds) {
		String formattedLeftTime = OsmAndFormatter.getFormattedDurationShortMinutes(leftSeconds);
		setText(formattedLeftTime, getUnits(leftSeconds));
	}

	@Nullable
	private String getUnits(long timeLeft) {
		if (timeLeft >= 0) {
			long diffInMinutes = TimeUnit.MINUTES.convert(timeLeft, TimeUnit.SECONDS);
			String hour = app.getString(R.string.int_hour);
			String minute = app.getString(R.string.shared_string_minute_lowercase);
			return diffInMinutes >= 60 ? hour : minute;
		}
		return null;
	}

	@Nullable
	protected String getAdditionalWidgetName() {
		if (widgetState != null/* && arrivalTimeOtherwiseTimeToGoPref != null*/) {
			return getString(getCurrentState().titleId);
		}
		return null;
	}

	@Nullable
	protected String getWidgetName() {
		if (widgetState != null) {
			DiffElevationToPointWidgetState.DiffElevationType state = getCurrentState();
			//if (state == TimeToNavigationPointState.INTERMEDIATE_ARRIVAL_TIME || state == TimeToNavigationPointState.INTERMEDIATE_TIME_TO_GO) {
			if (state == DiffElevationToPointWidgetState.DiffElevationType.POSITIVE_DIFF) {
					return getString(R.string.rendering_attr_smoothness_intermediate_name);
			} else {
				return getString(R.string.route_descr_destination);
			}
		}
		return super.getWidgetName();
	}

	@NonNull
	private DiffElevationToPointWidgetState.DiffElevationType getCurrentState() {
		return widgetState.getDiffElevationType();
	}
	/*
	@NonNull
	private DiffElevationToPointWidgetState getCurrentState() {
		return DiffElevationToPointWidgetState.getState(widgetState.isIntermediate(), arrivalTimeOtherwiseTimeToGoPref.get());
	} */
}