
package robot.commands.auto;

import edu.wpi.first.wpilibj.command.Command;
import robot.Robot;
import robot.RobotConst;

/**
 *  The DriveOnHeading command is the base class for all of the 
 *  auto commands as it tracks a gyro heading.
 *  <p>
 *  This command should be extended for other types of auto drive commands.
 */
public abstract class DriveOnHeadingCommand extends Command {

	private enum Step { COARSE, FINE };
	
	protected double heading;
	protected double setSpeed;
	
	private Step step = Step.COARSE;
	
	/**
	 * Drive on the specified heading at the specified speed.
	 * <p>
	 * This Command is used as the base for all Auto commands and 
	 * should be extended to stop using the isFinished method.
	 * @param heading to drive in degrees (0 to 360)
	 * @param speed to drive (-1.0 to 1.0).
	 */
    public DriveOnHeadingCommand(double heading, double speed) {
        // Use requires() here to declare subsystem dependencies
        requires(Robot.chassisSubsystem);
        this.heading  = heading;
        this.setSpeed = speed;
    }

    // Called just before this Command runs the first time
    @Override
    protected void initialize() {
    	
    	// Coarse adjustment is used if the error is > 30 degrees.  This will
    	// cause the robot to pivot before continuing on the selected direction.
    	step = Step.COARSE;
    	
    	double angleError = Robot.chassisSubsystem.getAngleError(heading);
    	
    	if (Math.abs(angleError) < 30.0d) { 
    		step = Step.FINE;
    		enableGyroPid();
    	}
    }

    // Called repeatedly when this Command is scheduled to run
    @Override
    protected void execute() {

    	double leftSpeed  = 0d;
    	double rightSpeed = 0d;
    	
    	double angleError = Robot.chassisSubsystem.getAngleError(heading);
    	
    	switch (step) {
    	
    	case COARSE:
    		
        	// In the coarse step, the robot pivots in place turning one wheel forward
        	// and the other wheel back until the robot angle is within the range
    		// to use the PID controller.  The PID controller is not used outside of this
    		// range because the idea is to have the robot travelling in the direction
    		// of the heading, so we do not want to move forward until the direction
    		// is approximately correct.
    		
    		// Turn off the pid control for the gyro.
    		Robot.chassisSubsystem.disableGyroPid();
    		
        	if (Math.abs(angleError) < 30.0d) { 
        		step = Step.FINE;
        		enableGyroPid();
        		break;
        	}

        	// Pivot based on the error direction
        	if (angleError > 0d) {
        		
        		// If the angle error is negative, then turn clockwise to close the error
        		leftSpeed  =   RobotConst.GYRO_PIVOT_MAX_SPEED;
        		rightSpeed = - RobotConst.GYRO_PIVOT_MAX_SPEED;
        		
        	} else {
        		
        		// If the angle error is negative, then turn clockwise to close the error
        		leftSpeed  = - RobotConst.GYRO_PIVOT_MAX_SPEED;
        		rightSpeed =   RobotConst.GYRO_PIVOT_MAX_SPEED;

        	}
        	break;
			
    	case FINE:

    		// Get the PID output and use it to steer the robot by adjusting the speed
    		// downward on the appropriate side of the robot to bring it back into 
    		// alignment.
    		double gyroPidOutput = Robot.chassisSubsystem.getGyroPidOutput();
    		
    		leftSpeed  = setSpeed;
    		rightSpeed = setSpeed;
    		
    		// FIXME:
    		// Slow down one motor based on the error.
			if (gyroPidOutput > 0) {

	    		rightSpeed -= gyroPidOutput;
	    		
	    		if (rightSpeed < -setSpeed) {
	    			 rightSpeed = -setSpeed;
	    		}
	    		
	    	}
	    	else {
	    		leftSpeed +=  gyroPidOutput;

	    		if (leftSpeed < -setSpeed) {
	    			leftSpeed = -setSpeed;
	    		}
	    	}
    		break;
    		
    	}
    	
    	Robot.chassisSubsystem.setMotorSpeeds(leftSpeed, rightSpeed);
    }

    
    @Override
    protected void end() {
    	Robot.chassisSubsystem.disableGyroPid();
    } 
    
    private void enableGyroPid() {
    	
		// Enable the Gyro PID
		Robot.chassisSubsystem.enableGyroPid();
		Robot.chassisSubsystem.setGyroPidSetpoint(heading);
		
    }
    
    
}
