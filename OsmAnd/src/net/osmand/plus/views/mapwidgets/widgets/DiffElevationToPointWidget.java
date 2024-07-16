package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.DIFF_ELEVATION_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.DIFF_ELEVATION_TO_INTERMEDIATE;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetstates.DiffElevationToPointWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.concurrent.TimeUnit;

public class DiffElevationToPointWidget extends SimpleWidget {

	private static final long UPDATE_INTERVAL_DIFF_ELEVATION = 10;

	private final RoutingHelper routingHelper;
	private final DiffElevationToPointWidgetState widgetState;
	private DiffElevationToPointWidgetState.DiffElevationDisplay cachedDisplayPreference;
	private double cachedRemaining;

	public DiffElevationToPointWidget(@NonNull MapActivity mapActivity, @NonNull DiffElevationToPointWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, getWidgetType(widgetState), customId, widgetsPanel);
		this.widgetState = widgetState;
		this.routingHelper = app.getRoutingHelper();
		this.cachedDisplayPreference = widgetState.getDiffElevationDisplay();

		setText(null, null);
		updateIcons();
		updateContentTitle();
		setOnClickListener(getOnClickListener());
		updateWidgetName();
	}

	private static WidgetType getWidgetType(DiffElevationToPointWidgetState widgetState) {
		switch (widgetState.getDiffElevationTarget()) {
			case DESTINATION:
				return DIFF_ELEVATION_TO_DESTINATION;
			case NEXT_INTERMEDIATE:
				return DIFF_ELEVATION_TO_INTERMEDIATE;
			default: // TODO
				return null;
		}
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
		double remaining = 0;

		boolean modeUpdated = widgetState.getDiffElevationDisplay() != cachedDisplayPreference;
		if (modeUpdated) {
			cachedDisplayPreference = widgetState.getDiffElevationDisplay();
			updateIcons();
			updateContentTitle();
		}

		if (routingHelper.isRouteCalculated()) {
			remaining = routingHelper.getLeftDiffElevation().total();
			boolean updateIntervalPassed = Math.abs(remaining - cachedRemaining) > UPDATE_INTERVAL_DIFF_ELEVATION;
			boolean shouldUpdate = remaining != 0 && (updateIntervalPassed || modeUpdated);
			if (shouldUpdate) {
				switch (widgetState.getDiffElevationTarget()) {
					case DESTINATION:
						updateDiffElevationToGo(routingHelper.getLeftDiffElevation(), cachedDisplayPreference);
						break;
					case NEXT_INTERMEDIATE:
						updateDiffElevationToGo(routingHelper.getLeftDiffElevationNextIntermediate(), cachedDisplayPreference);
						break;
				}
			}
		}

		if (remaining == 0 && cachedRemaining != 0) {
			cachedRemaining = 0;
			setText(null, null);
		}
	}

	private void updateIcons() {
		setIcons(widgetState.getSettingsIconId(false), widgetState.getSettingsIconId(true));
	}

	private void updateContentTitle() {
		setContentTitle(widgetState.getTitle());
	}

	private void updateDiffElevationToGo(RouteCalculationResult.DiffElevation diffElevation, DiffElevationToPointWidgetState.DiffElevationDisplay cachedTypePreference) {
		String formattedAltUp = OsmAndFormatter.getFormattedAlt(diffElevation.up, app);
		String formattedAltDown = OsmAndFormatter.getFormattedAlt(diffElevation.down, app);
		switch(cachedTypePreference) {
			case BOTH_DIFF:
				setText("+" + formattedAltUp, "-" + formattedAltDown);
				break;
			case POSITIVE_DIFF:
				setText(formattedAltUp, "D+");
				break;
			case NEGATIVE_DIFF:
				setText(formattedAltDown,"D-");
				break;
		}
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
			return widgetState.getTitle();
		}
		return null;
	}

	@Nullable
	protected String getWidgetName() {
		if (widgetState != null) {
			DiffElevationToPointWidgetState.DiffElevationDisplay state = getCurrentState();
			//if (state == TimeToNavigationPointState.INTERMEDIATE_ARRIVAL_TIME || state == TimeToNavigationPointState.INTERMEDIATE_TIME_TO_GO) {
			if (state == DiffElevationToPointWidgetState.DiffElevationDisplay.POSITIVE_DIFF) {
					return getString(R.string.rendering_attr_smoothness_intermediate_name);
			} else {
				return getString(R.string.route_descr_destination);
			}
		}
		return super.getWidgetName();
	}

	@NonNull
	private DiffElevationToPointWidgetState.DiffElevationDisplay getCurrentState() {
		return widgetState.getDiffElevationDisplay();
	}

}