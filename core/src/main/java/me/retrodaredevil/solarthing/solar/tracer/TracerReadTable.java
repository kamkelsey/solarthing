package me.retrodaredevil.solarthing.solar.tracer;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.retrodaredevil.io.serial.SerialConfig;
import me.retrodaredevil.io.serial.SerialConfigBuilder;
import me.retrodaredevil.solarthing.annotations.JsonExplicit;
import me.retrodaredevil.solarthing.annotations.NotNull;
import me.retrodaredevil.solarthing.annotations.SerializeNameDefinedInBase;
import me.retrodaredevil.solarthing.packets.Mode;
import me.retrodaredevil.solarthing.packets.Modes;
import me.retrodaredevil.solarthing.packets.support.Support;
import me.retrodaredevil.solarthing.solar.common.AdvancedAccumulatedChargeController;
import me.retrodaredevil.solarthing.solar.common.BasicChargeController;
import me.retrodaredevil.solarthing.solar.common.ErrorReporter;
import me.retrodaredevil.solarthing.solar.common.RecordBatteryVoltage;
import me.retrodaredevil.solarthing.solar.tracer.mode.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Set;

@JsonExplicit
public interface TracerReadTable extends RecordBatteryVoltage, BasicChargeController, AdvancedAccumulatedChargeController, ErrorReporter {

	SerialConfig SERIAL_CONFIG = new SerialConfigBuilder(115200)
			.setDataBits(8)
			.setStopBits(SerialConfig.StopBits.ONE)
			.setParity(SerialConfig.Parity.NONE)
			.build();

	@Override
	default @NotNull Mode getChargingMode() {
		throw new UnsupportedOperationException("TODO - We need to do this. We will probably use a combination of ChargingStatus and something else for this as charging status doesn't have a bulk mode");
	}

	@Override
	default int getDailyAH() { return 0; }
	@Override
	default @NotNull Support getDailyAHSupport() { return Support.NOT_SUPPORTED; }

	@JsonProperty("ratedInputVoltage")
	float getRatedInputVoltage();
	@JsonProperty("ratedInputCurrent")
	float getRatedInputCurrent();
	@JsonProperty("ratedInputPower")
	float getRatedInputPower();


	@JsonProperty("ratedOutputVoltage")
	float getRatedOutputVoltage(); // basically the nominal battery voltage (I think)
	@JsonProperty("ratedOutputCurrent")
	float getRatedOutputCurrent();
	@JsonProperty("ratedOutputPower")
	float getRatedOutputPower();

	@JsonProperty("chargingTypeValue")
	int getChargingTypeValue(); // 0x3008
	default @NotNull TracerChargingType getChargingType() { return Modes.getActiveMode(TracerChargingType.class, getChargingTypeValue()); }

	@JsonProperty("ratedLoadOutputCurrent")
	float getRatedLoadOutputCurrent();

	// ===

	@SerializeNameDefinedInBase
	@Override
	@NotNull Float getPVVoltage(); // 0x3100
	@SerializeNameDefinedInBase
	@Override
	@NotNull Float getPVCurrent();

	@JsonProperty("pvWattage")
	@Override
	@NotNull Float getPVWattage();

	@SerializeNameDefinedInBase
	@Override
	float getBatteryVoltage(); // 0x3104

	@JsonProperty("chargingCurrent")
	@Override
	@NotNull Float getChargingCurrent();
	// Page 2
	@JsonProperty("chargingPower")
	@Override
	@NotNull Float getChargingPower();

	@JsonProperty("loadVoltage")
	float getLoadVoltage();
	@JsonProperty("loadCurrent")
	float getLoadCurrent();
	@JsonProperty("loadPower")
	float getLoadPower();

	@JsonProperty("batteryTemperatureCelsius")
	float getBatteryTemperatureCelsius(); // 0x3110
	@JsonProperty("insideControllerTemperatureCelsius")
	float getInsideControllerTemperatureCelsius();
	@JsonProperty("powerComponentTemperatureCelsius")
	float getPowerComponentTemperatureCelsius();

	@JsonProperty("batterySOC")
	int getBatterySOC(); // TODO Is the raw range of this 0 to 100? Or 0 to (100 * 100)? If 0 to (100 * 100), then we should make this a double, maybe make it a double anyway

	@JsonProperty("remoteBatteryTemperatureCelsius")
	float getRemoteBatteryTemperatureCelsius();

	@JsonProperty("realBatteryRatedVoltageValue")
	int getRealBatteryRatedVoltageValue();

	// ===

	@JsonProperty("batteryStatusValue")
	int getBatteryStatusValue();
	default int getBatteryVoltageStatusValue() { return getBatteryStatusValue() & 0b1111; }
	default int getBatteryTemperatureStatusValue() { return getBatteryStatusValue() & 0b11110000; }
	default boolean isBatteryInternalResistanceAbnormal() { return (getBatteryStatusValue() & 0b100000000) != 0; } // check bit8
	default boolean isBatteryWrongIdentificationForRatedVoltage() { return ((getBatteryStatusValue() >> 15) & 1) != 0; } // check bit15
	default @NotNull TracerBatteryVoltageStatus getBatteryVoltageStatus() { return Modes.getActiveMode(TracerBatteryVoltageStatus.class, getBatteryVoltageStatusValue()); }
	default @NotNull TracerBatteryTemperatureStatus getBatteryTemperatureStatus() { return Modes.getActiveMode(TracerBatteryTemperatureStatus.class, getBatteryTemperatureStatusValue()); }

	@JsonProperty("chargingEquipmentStatusValue")
	int getChargingEquipmentStatus();
	default int getInputVoltageStatusValue() { return getChargingEquipmentStatus() >> 14; }
	default int getChargingStatusValue() { return (getChargingEquipmentStatus() >> 2) & 0b11; }
	@Override
	default @NotNull Set<ChargingEquipmentError> getErrorModes() { return Modes.getActiveModes(ChargingEquipmentError.class, getChargingEquipmentStatus()); }
	/**
	 * @deprecated Use {@link #getChargingEquipmentStatus()} instead
	 * @return {@link #getChargingEquipmentStatus()}
	 */
	@Deprecated
	@Override
	default int getErrorModeValue() { return getChargingEquipmentStatus(); }
	default boolean isRunning() { return (getChargingEquipmentStatus() & 1) == 1; }
	default @NotNull InputVoltageStatus getInputVoltageStatus() { return Modes.getActiveMode(InputVoltageStatus.class, getInputVoltageStatusValue()); }
	default @NotNull ChargingStatus getChargingStatus() { return Modes.getActiveMode(ChargingStatus.class, getChargingStatusValue()); }

	// Page 3
	// region Read Only Accumulators + Extra
	@JsonProperty("dailyMaxInputVoltage")
	float getDailyMaxPVVoltage();
	@JsonProperty("dailyMinInputVoltage")
	float getDailyMinPVVoltage();
	@SerializeNameDefinedInBase
	@Override
	float getDailyMaxBatteryVoltage(); // 0x3302
	@SerializeNameDefinedInBase
	@Override
	float getDailyMinBatteryVoltage();
	@SerializeNameDefinedInBase
	@Override
	float getDailyKWHConsumption();
	@JsonProperty("monthlyKWHConsumption")
	float getMonthlyKWHConsumption();
	@JsonProperty("yearlyKWHConsumption")
	float getYearlyKWHConsumption();
	@SerializeNameDefinedInBase
	@Override
	float getCumulativeKWHConsumption();
	@SerializeNameDefinedInBase
	@Override
	float getDailyKWH(); // 0x330C
	@JsonProperty("monthlyKWH")
	float getMonthlyKWH();
	@JsonProperty("yearlyKWH")
	float getYearlyKWH();
	@SerializeNameDefinedInBase
	@Override
	float getCumulativeKWH();
	@JsonProperty("carbonDioxideReductionTons")
	float getCarbonDioxideReductionTons();

	@JsonProperty("netBatteryCurrent")
	float getNetBatteryCurrent();
	@JsonProperty("batteryTemperatureCelsius331D")
	float getBatteryTemperatureCelsius331D(); // 0x331D
	@JsonProperty("ambientTemperatureCelsius")
	float getAmbientTemperatureCelsius();
	// endregion

	// region Read-write settings
	@JsonProperty("batteryTypeValue")
	int getBatteryTypeValue(); // 0x9000
	@JsonProperty("batteryCapacityAmpHours")
	int getBatteryCapacityAmpHours();
	@JsonProperty("temperatureCompensationCoefficient")
	int getTemperatureCompensationCoefficient();
	@JsonProperty("highVoltageDisconnect")
	float getHighVoltageDisconnect();
	@JsonProperty("chargingLimitVoltage")
	float getChargingLimitVoltage();
	@JsonProperty("overVoltageReconnect")
	float getOverVoltageReconnect();
	@JsonProperty("equalizationVoltage")
	float getEqualizationVoltage();
	@JsonProperty("boostVoltage")
	float getBoostVoltage();
	@JsonProperty("floatVoltage")
	float getFloatVoltage();
	@JsonProperty("boostReconnectVoltage")
	float getBoostReconnectVoltage();
	@JsonProperty("lowVoltageReconnect")
	float getLowVoltageReconnect();
	@JsonProperty("underVoltageRecover")
	float getUnderVoltageRecover();
	@JsonProperty("underVoltageWarning")
	float getUnderVoltageWarning();
	@JsonProperty("lowVoltageDisconnect")
	float getLowVoltageDisconnect();
	@JsonProperty("dischargingLimitVoltage")
	float getDischargingLimitVoltage();

	// Minutes and Seconds on 0x9013

	/** @return 48 bit number representing a real time clock. Low 8 bits represent seconds, ..., high 8 bits represent year */
	@JsonProperty("secondMinuteHourDayMonthYearRaw")
	long getSecondMinuteHourDayMonthYearRaw();
	default LocalTime getClockTime() {
		return TracerUtil.convertTracer48BitRawTimeToLocalTime(getSecondMinuteHourDayMonthYearRaw());
	}
	default MonthDay getClockMonthDay() {
		return TracerUtil.extractTracer48BitRawInstantToMonthDay(getSecondMinuteHourDayMonthYearRaw());
	}
	default int getClockYearNumber() {
		return TracerUtil.extractTracer48BitRawInstantToYearNumber(getSecondMinuteHourDayMonthYearRaw());
	}

	@JsonProperty("equalizationChargingCycleDays")
	int getEqualizationChargingCycleDays();
	@JsonProperty("batteryTemperatureWarningUpperLimit")
	float getBatteryTemperatureWarningUpperLimit();
	@JsonProperty("batteryTemperatureWarningLowerLimit")
	float getBatteryTemperatureWarningLowerLimit();
	@JsonProperty("insideControllerTemperatureWarningUpperLimit")
	float getInsideControllerTemperatureWarningUpperLimit();
	@JsonProperty("insideControllerTemperatureWarningUpperLimitRecover")
	float getInsideControllerTemperatureWarningUpperLimitRecover();
	@JsonProperty("powerComponentTemperatureWarningUpperLimit")
	float getPowerComponentTemperatureUpperLimit();
	@JsonProperty("powerComponentTemperatureWarningUpperLimitRecover")
	float getPowerComponentTemperatureUpperLimitRecover();
	@JsonProperty("lineImpedance")
	float getLineImpedance(); // 0x901D // milliohms
	@JsonProperty("nightPVVoltageThreshold")
	float getNightPVVoltageThreshold();
	@JsonProperty("lightSignalStartupDelayTime")
	int getLightSignalStartupDelayTime();
	@JsonProperty("dayPVVoltageThreshold")
	float getDayPVVoltageThreshold();
	@JsonProperty("lightSignalTurnOffDelayTime")
	int getLightSignalTurnOffDelayTime();
	@JsonProperty("loadControlModeValue")
	int getLoadControlModeValue();
	default LoadControlMode getLoadControlMode() { return Modes.getActiveMode(LoadControlMode.class, getLoadControlModeValue()); }

	@JsonProperty("workingTimeLength1Raw")
	int getWorkingTimeLength1Raw();
	default Duration getWorkingTime1Length() { return TracerUtil.convertTracerDurationRawToDuration(getWorkingTimeLength1Raw()); }
	@JsonProperty("workingTimeLength2Raw")
	int getWorkingTimeLength2Raw();
	default Duration getWorkingTime2Length() { return TracerUtil.convertTracerDurationRawToDuration(getWorkingTimeLength2Raw()); }

	@JsonProperty("turnOnTiming1Raw")
	long getTurnOnTiming1Raw();
	default LocalTime getTurnOnTiming1() { return TracerUtil.convertTracer48BitRawTimeToLocalTime(getTurnOnTiming1Raw()); }
	@JsonProperty("turnOffTiming1Raw")
	long getTurnOffTiming1Raw();
	default LocalTime getTurnOffTiming1() { return TracerUtil.convertTracer48BitRawTimeToLocalTime(getTurnOffTiming1Raw()); }
	@JsonProperty("turnOnTiming2Raw")
	long getTurnOnTiming2Raw();
	default LocalTime getTurnOnTiming2() { return TracerUtil.convertTracer48BitRawTimeToLocalTime(getTurnOnTiming2Raw()); }
	@JsonProperty("turnOffTiming2Raw")
	long getTurnOffTiming2Raw();
	default LocalTime getTurnOffTiming2() { return TracerUtil.convertTracer48BitRawTimeToLocalTime(getTurnOffTiming2Raw()); }
	@JsonProperty("lengthOfNightRaw")
	int getLengthOfNightRaw();
	default Duration getLengthOfNight() { return TracerUtil.convertTracerDurationRawToDuration(getLengthOfNightRaw()); }

	@JsonProperty("batteryRatedVoltageCode")
	int getBatteryRatedVoltageCode();
	default @NotNull BatteryDetection getBatteryDetection() { return Modes.getActiveMode(BatteryDetection.class, getBatteryRatedVoltageCode()); }
	@JsonProperty("loadTimingControlSelectionValueRaw")
	int getLoadTimingControlSelectionValueRaw();
	@JsonProperty("isLoadOnByDefaultInManualMode")
	boolean isLoadOnByDefaultInManualMode(); // 0x906A
	@JsonProperty("equalizeDurationMinutes")
	int getEqualizeDurationMinutes();
	@JsonProperty("boostDurationMinutes")
	int getBoostDurationMinutes();
	@JsonProperty("dischargingPercentage")
	int getDischargingPercentage();
	@JsonProperty("chargingPercentage")
	int getChargingPercentage();
	@JsonProperty("batteryManagementModeValue")
	int getBatteryManagementModeValue();
	default @NotNull BatteryManagementMode getBatteryManagementMode() { return Modes.getActiveMode(BatteryManagementMode.class, getBatteryManagementModeValue()); }

	@JsonProperty("isManualLoadControlOn")
	boolean isManualLoadControlOn();
	@JsonProperty("isLoadTestModeEnabled")
	boolean isLoadTestModeEnabled();
	@JsonProperty("isLoadForcedOn")
	boolean isLoadForcedOn();
	@JsonProperty("isInsideControllerOverTemperature")
	boolean isInsideControllerOverTemperature();
	@JsonProperty("isNight")
	boolean isNight();

	// endregion

}
