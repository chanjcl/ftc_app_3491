package org.firstinspires.ftc.teamcode.gamecode;

import android.support.annotation.Nullable;
import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.vuforia.Image;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Vuforia;

import org.firstinspires.ftc.robotcontroller.internal.GlobalValuesActivity;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.teamcode.R;
import org.firstinspires.ftc.teamcode.RC;
import org.firstinspires.ftc.teamcode.opmodesupport.AutoOpMode;
import org.firstinspires.ftc.teamcode.robots.Robot;
import org.firstinspires.ftc.teamcode.util.MathUtils;
import org.firstinspires.ftc.teamcode.robots.Fermion;
import org.firstinspires.ftc.teamcode.util.VortexUtils;

import static org.firstinspires.ftc.teamcode.util.VortexUtils.getImageFromFrame;

/**
 * Created by FIXIT on 16-10-21.
 */
@Autonomous
public class FermionBlueShot extends AutoOpMode {

    @Override
    public void runOp() throws InterruptedException {
        final Fermion muon = new Fermion(true);

        VuforiaLocalizer.Parameters params = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
        params.vuforiaLicenseKey = RC.VUFORIA_LICENSE_KEY;
        params.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;

        VuforiaLocalizer locale = ClassFactory.createVuforiaLocalizer(params);
        locale.setFrameQueueCapacity(1);
        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

        VuforiaTrackables beacons = locale.loadTrackablesFromAsset("FTC_2016-17");
        VuforiaTrackableDefaultListener wheels = (VuforiaTrackableDefaultListener) beacons.get(0).getListener();
        VuforiaTrackableDefaultListener legos = (VuforiaTrackableDefaultListener) beacons.get(2).getListener();

        muon.startShooterControl();
        muon.prime();
        waitForStart();
        beacons.activate();
        muon.addVeerCheckRunnable();
        muon.resetTargetAngle();

        muon.right(1);
        sleep(1100);
        muon.stop();
        muon.shoot();

        if(RC.globalBool("2Balls")){
            muon.waitForState(Fermion.LOADED);
            muon.door.goToPos("open");
            muon.collector.setPower(-1);
            muon.shoot();
            sleep(1000);
            muon.door.goToPos("close");
            muon.setCollectorState(Robot.STOP);
        }//if

        muon.waitForState(Fermion.FIRE);


        muon.imuTurnL(195, 0.5);

        muon.forward(0.2);
        sleep(1000);
        muon.forward(0.09);

        while (wheels.getPose() == null && opModeIsActive()) {
            idle();
        }//while


        VectorF trans = wheels.getPose().getTranslation();

        Log.i("ANGLE", "HELLO" + Math.toDegrees(Math.atan2(trans.get(0), -trans.get(2))));

        double deg = Math.toDegrees(Math.atan2(trans.get(0), -trans.get(2)));
        if(deg < 0){
            muon.imuTurnL(-deg, 0.3);
        } else {
            muon.imuTurnR(deg, 0.3);
        }

        int config = VortexUtils.NOT_VISIBLE;
        try{
            config = VortexUtils.waitForBeaconConfig(
                    getImageFromFrame(locale.getFrameQueue().take(), PIXEL_FORMAT.RGB565),
                    wheels, locale.getCameraCalibration(), 5000);
            telemetry.addData("Beacon", config);
            Log.i(TAG, "runOp: " + config);
        } catch (Exception e){
            telemetry.addData("Beacon", "could not not be found");
        }

        Log.i(TAG, "runOp: before");
        muon.forward(1);
        sleep(600);
        Log.i(TAG, "runOp: after");

        muon.absoluteIMUTurn(90, 0.5);

        while (opModeIsActive() && muon.ultra.getDistance() < 100){
            muon.backward(0.2);
        }
        while(opModeIsActive() && muon.ultra.getDistance() > 457){
            muon.forward(0.2);
        }

        muon.stop();

        muon.left(0.2);

        int sensor = (config == VortexUtils.BEACON_BLUE_RED)? Robot.LEFT : Robot.RIGHT;
        while (opModeIsActive() && muon.getLight(sensor) < muon.LIGHT_THRESHOLD){
            Log.i("light", "" + muon.getLight(sensor));
        }
        muon.stop();
        sleep(100);
        muon.forward(0.5);
        sleep(700);
        muon.stop();
        muon.backward(0.5);
        sleep(500);


        muon.absoluteIMUTurn(90, 0.5);


        //------------------------------Beacon 2--------------

        muon.left(1);
        sleep(1500);

        while (opModeIsActive() && muon.ultra.getDistance() < 50){
            muon.backward(0.2);
        }
        while(opModeIsActive() && muon.ultra.getDistance() > 457){
            muon.forward(0.2);
        }

        muon.left(0.2);

        sensor = Robot.LEFT;
        while (opModeIsActive() && muon.getLight(sensor) < muon.LIGHT_THRESHOLD){
            Log.i("light", "" + muon.getLight(sensor));
        }
        muon.stop();
        muon.absoluteIMUTurn(90, 0.5);
        muon.stop();

        long timeBack = 0;
        clearTimer();
        while (legos.getPose() == null && opModeIsActive()) {
            if(getMilliSeconds() > 1000){
                Log.i(TAG, "runOp: " + "can't see");
                muon.backward(0.3);
                sleep(300);
                timeBack += 300;
                muon.stop();
                clearTimer();
            }
            idle();
        }//while



        config = VortexUtils.NOT_VISIBLE;
        try{
            config = VortexUtils.waitForBeaconConfig(
                    getImageFromFrame(locale.getFrameQueue().take(), PIXEL_FORMAT.RGB565),
                    legos, locale.getCameraCalibration(), 5000);
            telemetry.addData("Beacon", config);
            Log.i(TAG, "runOp: " + config);
        } catch (Exception e){
            telemetry.addData("Beacon", "could not not be found");
        }

        while(opModeIsActive() && muon.ultra.getDistance() > 300){
            muon.forward(0.2);
        }

        if(config == VortexUtils.BEACON_RED_BLUE){
            muon.stop();
            muon.left(0.2);

            sensor = Robot.RIGHT;
            while (opModeIsActive() && muon.getLight(sensor) < muon.LIGHT_THRESHOLD){
                Log.i("light", "" + muon.getLight(sensor));
            }
            muon.stop();
            sleep(100);
        }

        muon.forward(0.3);
        sleep(1000);
        muon.stop();
        muon.backward(0.5);
        sleep(300);
        muon.stop();

    }//runOp

}
