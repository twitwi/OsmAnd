package net.osmand.plus.charts;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM;
import static net.osmand.plus.charts.GPXDataSetAxisType.DISTANCE;
import static net.osmand.plus.charts.GPXDataSetAxisType.TIME;
import static net.osmand.plus.charts.GPXDataSetAxisType.TIME_OF_DAY;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.PointAttribute;
import net.osmand.gpx.PointAttribute.Elevation;
import net.osmand.gpx.PointAttribute.Speed;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ChartUtils {

	public static final int CHART_LABEL_COUNT = 3;
	private static final int MAX_CHART_DATA_ITEMS = 10000;

	public static void setupGPXChart(@NonNull LineChart mChart) {
		setupGPXChart(mChart, 24f, 16f, true);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, float topOffset, float bottomOffset,
	                                 boolean useGesturesAndScale) {
		setupGPXChart(mChart, topOffset, bottomOffset, useGesturesAndScale, null);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, float topOffset, float bottomOffset,
	                                 boolean useGesturesAndScale, @Nullable Drawable markerIcon) {
		GpxMarkerView markerView = new GpxMarkerView(mChart.getContext(), markerIcon);
		setupGPXChart(mChart, markerView, topOffset, bottomOffset, useGesturesAndScale);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, @NonNull GpxMarkerView markerView,
	                                 float topOffset, float bottomOffset, boolean useGesturesAndScale) {
		Context context = mChart.getContext();

		mChart.setHardwareAccelerationEnabled(true);
		mChart.setTouchEnabled(useGesturesAndScale);
		mChart.setDragEnabled(useGesturesAndScale);
		mChart.setScaleEnabled(useGesturesAndScale);
		mChart.setPinchZoom(useGesturesAndScale);
		mChart.setScaleYEnabled(false);
		mChart.setAutoScaleMinMaxEnabled(true);
		mChart.setDrawBorders(false);
		mChart.getDescription().setEnabled(false);
		mChart.setMaxVisibleValueCount(10);
		mChart.setMinOffset(0f);
		mChart.setDragDecelerationEnabled(false);

		mChart.setExtraTopOffset(topOffset);
		mChart.setExtraBottomOffset(bottomOffset);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		markerView.setChartView(mChart); // For bounds control
		mChart.setMarker(markerView); // Set the marker to the chart
		mChart.setDrawMarkers(true);

		ChartLabel chartLabel = new ChartLabel(context, R.layout.chart_label);
		chartLabel.setChart(mChart);
		mChart.setYAxisLabelView(chartLabel);

		int xAxisRulerColor = ContextCompat.getColor(context, R.color.gpx_chart_black_grid);
		int labelsColor = ContextCompat.getColor(context, R.color.description_font_and_bottom_sheet_icons);
		XAxis xAxis = mChart.getXAxis();
		xAxis.setDrawAxisLine(true);
		xAxis.setDrawAxisLineBehindData(false);
		xAxis.setAxisLineWidth(1);
		xAxis.setAxisLineColor(xAxisRulerColor);
		xAxis.setDrawGridLines(true);
		xAxis.setDrawGridLinesBehindData(false);
		xAxis.setGridLineWidth(1.5f);
		xAxis.setGridColor(xAxisRulerColor);
		xAxis.enableGridDashedLine(25f, Float.MAX_VALUE, 0f);
		xAxis.setPosition(BOTTOM);
		xAxis.setTextColor(labelsColor);

		int dp4 = AndroidUtils.dpToPx(context, 4);
		int yAxisGridColor = AndroidUtils.getColorFromAttr(context, R.attr.chart_grid_line_color);

		YAxis leftYAxis = mChart.getAxisLeft();
		leftYAxis.enableGridDashedLine(dp4, dp4, 0f);
		leftYAxis.setGridColor(yAxisGridColor);
		leftYAxis.setGridLineWidth(1f);
		leftYAxis.setDrawBottomYGridLine(false);
		leftYAxis.setDrawAxisLine(false);
		leftYAxis.setDrawGridLinesBehindData(false);
		leftYAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		leftYAxis.setXOffset(16f);
		leftYAxis.setYOffset(-6f);
		leftYAxis.setLabelCount(CHART_LABEL_COUNT, true);

		YAxis rightYAxis = mChart.getAxisRight();
		rightYAxis.setDrawAxisLine(false);
		rightYAxis.setDrawGridLines(false);
		rightYAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		rightYAxis.setXOffset(16f);
		rightYAxis.setYOffset(-6f);
		rightYAxis.setLabelCount(CHART_LABEL_COUNT, true);
		rightYAxis.setEnabled(false);

		Legend legend = mChart.getLegend();
		legend.setEnabled(false);
	}

	private static float setupAxisDistance(OsmandApplication ctx, AxisBase axisBase, float meters) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		float divX;

		String format1 = "{0,number,0.#} ";
		String format2 = "{0,number,0.##} ";
		String fmt = null;
		float granularity = 1f;
		int mainUnitStr;
		float mainUnitInMeters;
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			mainUnitStr = R.string.nm;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}
		if (meters > 9.99f * mainUnitInMeters) {
			fmt = format1;
			granularity = .1f;
		}
		if (meters >= 100 * mainUnitInMeters ||
				meters > 9.99f * mainUnitInMeters ||
				meters > 0.999f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.NAUTICAL_MILES_AND_METERS && meters > 0.99f * mainUnitInMeters ||
				mc == MetricsConstants.NAUTICAL_MILES_AND_FEET && meters > 0.99f * mainUnitInMeters) {

			divX = mainUnitInMeters;
			if (fmt == null) {
				fmt = format2;
				granularity = .01f;
			}
		} else {
			fmt = null;
			granularity = 1f;
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				divX = 1f;
				mainUnitStr = R.string.m;
			} else if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				divX = 1f / FEET_IN_ONE_METER;
				mainUnitStr = R.string.foot;
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				divX = 1f / YARDS_IN_ONE_METER;
				mainUnitStr = R.string.yard;
			} else {
				divX = 1f;
				mainUnitStr = R.string.m;
			}
		}

		String formatX = fmt;
		String mainUnitX = ctx.getString(mainUnitStr);

		axisBase.setGranularity(granularity);
		axisBase.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatX)) {
				return MessageFormat.format(formatX + mainUnitX, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitX, ctx);
			}
		});

		return divX;
	}

	private static float setupXAxisTime(XAxis xAxis, long timeSpan) {
		boolean useHours = timeSpan / HOUR_IN_MILLIS > 0;
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter((value, axis) -> formatXAxisTime((int) (value + 0.5), useHours));
		return 1f;
	}

	public static String formatXAxisTime(int seconds, boolean useHours) {
		if (useHours) {
			return OsmAndFormatter.getFormattedDurationShort(seconds);
		} else {
			int minutes = (seconds / 60) % 60;
			int sec = seconds % 60;
			return (minutes < 10 ? "0" + minutes : minutes) + ":" + (sec < 10 ? "0" + sec : sec);
		}
	}

	private static float setupXAxisTimeOfDay(@NonNull XAxis xAxis, long startTime) {
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter((seconds, axis) -> {
			long time = startTime + (long) (seconds * 1000);
			return OsmAndFormatter.getFormattedFullTime(time);
		});
		return 1f;
	}

	private static List<Entry> calculateElevationArray(GPXTrackAnalysis analysis,
	                                                   GPXDataSetAxisType axisType,
	                                                   float divX, float convEle,
	                                                   boolean useGeneralTrackPoints,
	                                                   boolean calcWithoutGaps) {
		List<Entry> values = new ArrayList<>();
		List<Elevation> elevationData = analysis.getElevationData().getAttributes();
		float nextX = 0;
		float nextY;
		float elev;
		float prevElevOrig = -80000;
		float prevElev = 0;
		int i = -1;
		int lastIndex = elevationData.size() - 1;
		Entry lastEntry = null;
		float lastXSameY = -1;
		boolean hasSameY = false;
		float x = 0f;
		for (Elevation elevation : elevationData) {
			i++;
			if (axisType == TIME || axisType == TIME_OF_DAY) {
				x = elevation.timeDiff;
			} else {
				x = elevation.distance;
			}
			if (x >= 0) {
				if (!(calcWithoutGaps && elevation.firstPoint && lastEntry != null)) {
					nextX += x / divX;
				}
				if (elevation.hasValidValue()) {
					elev = elevation.value;
					if (prevElevOrig != -80000) {
						if (elev > prevElevOrig) {
							//elev -= 1f;
						} else if (prevElevOrig == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (prevElev == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (hasSameY) {
							values.add(new Entry(lastXSameY, lastEntry.getY()));
						}
						hasSameY = false;
					}
					if (useGeneralTrackPoints && elevation.firstPoint && lastEntry != null) {
						values.add(new Entry(nextX, lastEntry.getY()));
					}
					prevElevOrig = elevation.value;
					prevElev = elev;
					nextY = elev * convEle;
					lastEntry = new Entry(nextX, nextY);
					values.add(lastEntry);
				}
			}
		}
		return values;
	}

	public static void setupHorizontalGPXChart(OsmandApplication app, HorizontalBarChart chart, int yLabelsCount,
	                                           float topOffset, float bottomOffset, boolean useGesturesAndScale, boolean nightMode) {
		chart.setHardwareAccelerationEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
		chart.setTouchEnabled(useGesturesAndScale);
		chart.setDragEnabled(useGesturesAndScale);
		chart.setScaleYEnabled(false);
		chart.setAutoScaleMinMaxEnabled(true);
		chart.setDrawBorders(true);
		chart.getDescription().setEnabled(false);
		chart.setDragDecelerationEnabled(false);

		chart.setExtraTopOffset(topOffset);
		chart.setExtraBottomOffset(bottomOffset);

		XAxis xl = chart.getXAxis();
		xl.setDrawLabels(false);
		xl.setEnabled(false);
		xl.setDrawAxisLine(false);
		xl.setDrawGridLines(false);

		YAxis yl = chart.getAxisLeft();
		yl.setLabelCount(yLabelsCount);
		yl.setDrawLabels(false);
		yl.setEnabled(false);
		yl.setDrawAxisLine(false);
		yl.setDrawGridLines(false);
		yl.setAxisMinimum(0f);

		YAxis yr = chart.getAxisRight();
		yr.setLabelCount(yLabelsCount);
		yr.setDrawAxisLine(false);
		yr.setDrawGridLines(false);
		yr.setAxisMinimum(0f);
		chart.setMinOffset(0);

		int mainFontColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		yl.setTextColor(mainFontColor);
		yr.setTextColor(mainFontColor);

		chart.setFitBars(true);
		chart.setBorderColor(ColorUtilities.getDividerColor(app, nightMode));

		Legend l = chart.getLegend();
		l.setEnabled(false);
	}

	public static <E> BarData buildStatisticChart(@NonNull OsmandApplication app,
	                                              @NonNull HorizontalBarChart chart,
	                                              @NonNull RouteStatistics routeStatistics,
	                                              @NonNull GPXTrackAnalysis analysis,
	                                              boolean useRightAxis,
	                                              boolean nightMode) {

		XAxis xAxis = chart.getXAxis();
		xAxis.setEnabled(false);

		YAxis yAxis = getYAxis(chart, null, useRightAxis);
		float divX = setupAxisDistance(app, yAxis, analysis.totalDistance);

		List<RouteSegmentAttribute> segments = routeStatistics.elements;
		List<BarEntry> entries = new ArrayList<>();
		float[] stacks = new float[segments.size()];
		int[] colors = new int[segments.size()];
		for (int i = 0; i < stacks.length; i++) {
			RouteSegmentAttribute segment = segments.get(i);
			stacks[i] = segment.getDistance() / divX;
			colors[i] = segment.getColor();
		}
		entries.add(new BarEntry(0, stacks));
		BarDataSet barDataSet = new BarDataSet(entries, "");
		barDataSet.setColors(colors);
		barDataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		BarData dataSet = new BarData(barDataSet);
		dataSet.setDrawValues(false);
		dataSet.setBarWidth(1);
		chart.getAxisRight().setAxisMaximum(dataSet.getYMax());
		chart.getAxisLeft().setAxisMaximum(dataSet.getYMax());

		return dataSet;
	}

	public static OrderedLineDataSet createGPXElevationDataSet(@NonNull OsmandApplication app,
	                                                           @NonNull LineChart chart,
	                                                           @NonNull GPXTrackAnalysis analysis,
	                                                           @NonNull GPXDataSetType graphType,
	                                                           @NonNull GPXDataSetAxisType axisType,
	                                                           boolean useRightAxis,
	                                                           boolean drawFilled,
	                                                           boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean useFeet = settings.METRIC_SYSTEM.get().shouldUseFeet();
		float convEle = useFeet ? 3.28084f : 1.0f;

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		String mainUnitY = getMainUnitY(app, graphType);

		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(false));
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app));

		List<Entry> values = calculateElevationArray(analysis, axisType, divX, convEle, true, calcWithoutGaps);

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.ALTITUDE, axisType, !useRightAxis);
		dataSet.priority = (float) (analysis.avgElevation - analysis.minElevation) * convEle;
		dataSet.divX = divX;
		dataSet.mulY = convEle;
		dataSet.divY = Float.NaN;
		dataSet.units = mainUnitY;

		boolean nightMode = !settings.isLightContent();
		int color = ColorUtilities.getColor(app, graphType.getFillColorId(false));
		setupDataSet(app, dataSet, color, color, drawFilled, useRightAxis, nightMode);
		dataSet.setFillFormatter((ds, dataProvider) -> dataProvider.getYChartMin());

		return dataSet;
	}

	private static void setupDataSet(OsmandApplication app, OrderedLineDataSet dataSet,
	                                 @ColorInt int color, @ColorInt int fillColor,
	                                 boolean drawFilled, boolean useRightAxis, boolean nightMode) {
		dataSet.setColor(color);
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(fillColor);
		}
		dataSet.setDrawFilled(drawFilled);

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));

		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
	}

	public static OrderedLineDataSet createGPXSpeedDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		SpeedConstants speedConstants = settings.SPEED_SYSTEM.get();
		float mulSpeed = Float.NaN;
		float divSpeed = Float.NaN;
		String mainUnitY = getMainUnitY(app, graphType);
		if (speedConstants == SpeedConstants.KILOMETERS_PER_HOUR) {
			mulSpeed = 3.6f;
		} else if (speedConstants == SpeedConstants.MILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
		} else if (speedConstants == SpeedConstants.NAUTICALMILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
		} else if (speedConstants == SpeedConstants.MINUTES_PER_KILOMETER) {
			divSpeed = METERS_IN_KILOMETER / 60.0f;
		} else if (speedConstants == SpeedConstants.MINUTES_PER_MILE) {
			divSpeed = METERS_IN_ONE_MILE / 60.0f;
		} else {
			mulSpeed = 1f;
		}

		boolean speedInTrack = analysis.hasSpeedInTrack();
		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(!speedInTrack));
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setAxisMinimum(0f);

		ArrayList<Entry> values = new ArrayList<>();
		List<Speed> speedData = analysis.getSpeedData().getAttributes();
		float currentX = 0;

		for (int i = 0; i < speedData.size(); i++) {
			Speed speed = speedData.get(i);

			float stepX = axisType == TIME || axisType == TIME_OF_DAY ? speed.timeDiff : speed.distance;

			if (i == 0 || stepX > 0) {
				if (!(calcWithoutGaps && speed.firstPoint)) {
					currentX += stepX / divX;
				}

				float currentY = Float.isNaN(divSpeed) ? speed.value * mulSpeed : divSpeed / speed.value;
				if (currentY < 0 || Float.isInfinite(currentY)) {
					currentY = 0;
				}

				if (speed.firstPoint && currentY != 0) {
					values.add(new Entry(currentX, 0));
				}
				values.add(new Entry(currentX, currentY));
				if (speed.lastPoint && currentY != 0) {
					values.add(new Entry(currentX, 0));
				}
			}
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.SPEED, axisType, !useRightAxis);

		String format = null;
		if (dataSet.getYMax() < 3) {
			format = "{0,number,0.#} ";
		}
		String formatY = format;
		yAxis.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatY)) {
				return MessageFormat.format(formatY + mainUnitY, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app);
			}
		});

		if (Float.isNaN(divSpeed)) {
			dataSet.priority = analysis.avgSpeed * mulSpeed;
		} else {
			dataSet.priority = divSpeed / analysis.avgSpeed;
		}
		dataSet.divX = divX;
		if (Float.isNaN(divSpeed)) {
			dataSet.mulY = mulSpeed;
			dataSet.divY = Float.NaN;
		} else {
			dataSet.divY = divSpeed;
			dataSet.mulY = Float.NaN;
		}
		dataSet.units = mainUnitY;

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(!speedInTrack));
		setupDataSet(app, dataSet, color, color, drawFilled, useRightAxis, nightMode);

		return dataSet;
	}

	private static float getDivX(@NonNull OsmandApplication app, @NonNull LineChart lineChart,
	                             @NonNull GPXTrackAnalysis analysis, @NonNull GPXDataSetAxisType axisType,
	                             boolean calcWithoutGaps) {
		XAxis xAxis = lineChart.getXAxis();
		if (axisType == TIME && analysis.isTimeSpecified()) {
			return setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == TIME_OF_DAY && analysis.isTimeSpecified()) {
			return setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			return setupAxisDistance(app, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}
	}

	private static YAxis getYAxis(BarLineChartBase<?> chart, Integer textColor, boolean useRightAxis) {
		YAxis yAxis = useRightAxis ? chart.getAxisRight() : chart.getAxisLeft();
		yAxis.setEnabled(true);
		if (textColor != null) {
			yAxis.setTextColor(textColor);
		}
		return yAxis;
	}

	public static OrderedLineDataSet createGPXSlopeDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       @Nullable List<Entry> eleValues,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS) || (mc == MetricsConstants.NAUTICAL_MILES_AND_FEET);
		float convEle = useFeet ? 3.28084f : 1.0f;
		float totalDistance = calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		String mainUnitY = getMainUnitY(app, graphType);

		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(false));
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app));

		List<Entry> values;
		if (eleValues == null) {
			values = calculateElevationArray(analysis, DISTANCE, 1f, 1f, false, calcWithoutGaps);
		} else {
			values = new ArrayList<>(eleValues.size());
			for (Entry e : eleValues) {
				values.add(new Entry(e.getX() * divX, e.getY() / convEle));
			}
		}

		if (Algorithms.isEmpty(values)) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		int lastIndex = values.size() - 1;

		double STEP = 5;
		int l = 10;
		while (l > 0 && totalDistance / STEP > MAX_CHART_DATA_ITEMS) {
			STEP = Math.max(STEP, totalDistance / (values.size() * l--));
		}

		double[] calculatedDist = new double[(int) (totalDistance / STEP) + 1];
		double[] calculatedH = new double[(int) (totalDistance / STEP) + 1];
		int nextW = 0;
		for (int k = 0; k < calculatedDist.length; k++) {
			if (k > 0) {
				calculatedDist[k] = calculatedDist[k - 1] + STEP;
			}
			while (nextW < lastIndex && calculatedDist[k] > values.get(nextW).getX()) {
				nextW++;
			}
			double pd = nextW == 0 ? 0 : values.get(nextW - 1).getX();
			double ph = nextW == 0 ? values.get(0).getY() : values.get(nextW - 1).getY();
			calculatedH[k] = ph + (values.get(nextW).getY() - ph) / (values.get(nextW).getX() - pd) * (calculatedDist[k] - pd);
		}

		double SLOPE_PROXIMITY = Math.max(100, STEP * 2);

		if (totalDistance - SLOPE_PROXIMITY < 0) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		double[] calculatedSlopeDist = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];
		double[] calculatedSlope = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];

		int index = (int) ((SLOPE_PROXIMITY / STEP) / 2);
		for (int k = 0; k < calculatedSlopeDist.length; k++) {
			calculatedSlopeDist[k] = calculatedDist[index + k];
			calculatedSlope[k] = (calculatedH[2 * index + k] - calculatedH[k]) * 100 / SLOPE_PROXIMITY;
			if (Double.isNaN(calculatedSlope[k])) {
				calculatedSlope[k] = 0;
			}
		}

		List<Entry> slopeValues = new ArrayList<>(calculatedSlopeDist.length);
		float prevSlope = -80000;
		float slope;
		float x;
		float lastXSameY = 0;
		boolean hasSameY = false;
		Entry lastEntry = null;
		lastIndex = calculatedSlopeDist.length - 1;
		float timeSpanInSeconds = analysis.timeSpan / 1000f;
		for (int i = 0; i < calculatedSlopeDist.length; i++) {
			if ((axisType == TIME_OF_DAY || axisType == TIME) && analysis.isTimeSpecified()) {
				x = (timeSpanInSeconds * i) / calculatedSlopeDist.length;
			} else {
				x = (float) calculatedSlopeDist[i] / divX;
			}
			slope = (float) calculatedSlope[i];
			if (prevSlope != -80000) {
				if (prevSlope == slope && i < lastIndex) {
					hasSameY = true;
					lastXSameY = x;
					continue;
				}
				if (hasSameY) {
					slopeValues.add(new Entry(lastXSameY, lastEntry.getY()));
				}
				hasSameY = false;
			}
			prevSlope = slope;
			lastEntry = new Entry(x, slope);
			slopeValues.add(lastEntry);
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(slopeValues, "", GPXDataSetType.SLOPE, axisType, !useRightAxis);
		dataSet.divX = divX;
		dataSet.units = mainUnitY;

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(false));
		setupDataSet(app, dataSet, color, color, drawFilled, useRightAxis, nightMode);

		/*
		dataSet.setFillFormatter(new IFillFormatter() {
			@Override
			public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
				return dataProvider.getYChartMin();
			}
		});
		*/

		return dataSet;
	}


	@NonNull
	private static String getMainUnitY(@NonNull OsmandApplication app, @NonNull GPXDataSetType dataSetType) {
		OsmandSettings settings = app.getSettings();
		switch (dataSetType) {
			case ALTITUDE: {
				boolean shouldUseFeet = settings.METRIC_SYSTEM.get().shouldUseFeet();
				return app.getString(shouldUseFeet ? R.string.foot : R.string.m);
			}
			case SLOPE: {
				return "%";
			}
			case SPEED: {
				return settings.SPEED_SYSTEM.get().toShortString(app);
			}
			case SENSOR_HEART_RATE: {
				return app.getString(R.string.beats_per_minute_short);
			}
			case SENSOR_SPEED:
			case SENSOR_BIKE_POWER:
			case SENSOR_BIKE_CADENCE:
			case SENSOR_TEMPERATURE:
				return "";
		}
		return "";
	}

	public static List<ILineDataSet> getDataSets(LineChart chart,
	                                             OsmandApplication app,
	                                             GPXTrackAnalysis analysis,
	                                             @NonNull GPXDataSetType firstType,
	                                             @Nullable GPXDataSetType secondType,
	                                             boolean calcWithoutGaps) {
		if (app == null || chart == null || analysis == null) {
			return new ArrayList<>();
		}
		List<ILineDataSet> result = new ArrayList<>();
		if (secondType == null) {
			ILineDataSet dataSet = getDataSet(app, chart, analysis, firstType, calcWithoutGaps, false);
			if (dataSet != null) {
				result.add(dataSet);
			}
		} else {
			OrderedLineDataSet dataSet1 = getDataSet(app, chart, analysis, firstType, calcWithoutGaps, false);
			OrderedLineDataSet dataSet2 = getDataSet(app, chart, analysis, secondType, calcWithoutGaps, true);
			if (dataSet1 == null && dataSet2 == null) {
				return new ArrayList<>();
			} else if (dataSet1 == null) {
				result.add(dataSet2);
			} else if (dataSet2 == null) {
				result.add(dataSet1);
			} else if (dataSet1.getPriority() < dataSet2.getPriority()) {
				result.add(dataSet2);
				result.add(dataSet1);
			} else {
				result.add(dataSet1);
				result.add(dataSet2);
			}
		}
		return result;
	}

	@Nullable
	public static OrderedLineDataSet getDataSet(@NonNull OsmandApplication app,
	                                            @NonNull LineChart chart,
	                                            @NonNull GPXTrackAnalysis analysis,
	                                            @NonNull GPXDataSetType graphType,
	                                            boolean calcWithoutGaps,
	                                            boolean useRightAxis) {
		switch (graphType) {
			case ALTITUDE: {
				if (analysis.hasElevationData()) {
					return createGPXElevationDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SLOPE: {
				if (analysis.hasElevationData()) {
					return createGPXSlopeDataSet(app, chart, analysis, graphType, DISTANCE, null, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SPEED: {
				if (analysis.hasSpeedData()) {
					return createGPXSpeedDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_SPEED: {
				if (analysis.hasSensorSpeedData()) {
					return createSensorDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_HEART_RATE: {
				if (analysis.hasHeartRateData()) {
					return createSensorDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_BIKE_POWER: {
				if (analysis.hasBikePowerData()) {
					return createSensorDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_BIKE_CADENCE: {
				if (analysis.hasBikeCadenceData()) {
					return createSensorDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_TEMPERATURE: {
				if (analysis.hasTemperatureData()) {
					return createSensorDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
		}
		return null;
	}

	public static OrderedLineDataSet createSensorDataSet(@NonNull OsmandApplication app,
	                                                     @NonNull LineChart chart,
	                                                     @NonNull GPXTrackAnalysis analysis,
	                                                     @NonNull GPXDataSetType graphType,
	                                                     @NonNull GPXDataSetAxisType axisType,
	                                                     boolean useRightAxis,
	                                                     boolean drawFilled,
	                                                     boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(false));
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setAxisMinimum(0f);

		ArrayList<Entry> values = new ArrayList<>();
		List<PointAttribute> attributes = analysis.getAttributesData(graphType.getDataKey()).getAttributes();
		float currentX = 0;

		for (int i = 0; i < attributes.size(); i++) {
			PointAttribute attribute = attributes.get(i);

			float stepX = axisType == TIME || axisType == TIME_OF_DAY ? attribute.timeDiff : attribute.distance;

			if (i == 0 || stepX > 0) {
				if (!(calcWithoutGaps && attribute.firstPoint)) {
					currentX += stepX / divX;
				}

				float currentY = attribute.value.floatValue();
				if (currentY < 0 || Float.isInfinite(currentY)) {
					currentY = 0;
				}

				if (attribute.firstPoint && currentY != 0) {
					values.add(new Entry(currentX, 0));
				}
				values.add(new Entry(currentX, currentY));
				if (attribute.lastPoint && currentY != 0) {
					values.add(new Entry(currentX, 0));
				}
			}
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", graphType, axisType, !useRightAxis);

		String format = null;
		if (dataSet.getYMax() < 3) {
			format = "{0,number,0.#} ";
		}
		String formatY = format;
		String mainUnitY = getMainUnitY(app, graphType);
		yAxis.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatY)) {
				return MessageFormat.format(formatY + mainUnitY, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app);
			}
		});

		dataSet.divX = divX;
		dataSet.units = mainUnitY;

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(false));
		setupDataSet(app, dataSet, color, color, drawFilled, useRightAxis, nightMode);

		return dataSet;
	}
}