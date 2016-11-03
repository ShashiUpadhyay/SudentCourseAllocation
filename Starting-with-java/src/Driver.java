import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class Driver {

	public static String Preference_Input_File;
	public static String output_file_name;
	public static String delimiter = "\\s";

	public static int total_students = 12;
	public static int total_subjects = 4;
	public static int maximum_preference_score = 4;
	public static int maximum_seats_per_subject = 10;
	public static int total_subject_required_per_student = 3;
	public static int reshuffle = 0;
	public static double averagePreferenceScore;

	public static String[] subject_name_list = { "A", "B", "C", "D" };
	public static Student[][] student_matrix = new Student[total_students][total_subjects];
	public static Subject[] subject_matrix = new Subject[total_subjects];
	public static Student[] student_EnrollmentCount = new Student[total_students];

	public static int[] possible_subjects_required = new int[total_subjects];
	public static int[] students_subject_not_taken = new int[total_students];
	public static Map<Integer, int[]> student_subject_possibilites = new HashMap<Integer, int[]>();
	public static int[] students_reshuffle_count = new int[total_students];
	public static int[] student_preference_score = new int[total_students];

	public static void calculate_preference_score() {

		double total_preference_score = 0.0;
		for (int Student_reg = 0; Student_reg < total_students; Student_reg++) {
			int preferenceCount = 0;
			for (int subject = 0; subject < total_subjects; subject++) {
				preferenceCount = preferenceCount + student_matrix[Student_reg][subject].getPreferenceUsed();
			}
			student_preference_score[Student_reg] = preferenceCount;
			total_preference_score = total_preference_score + student_preference_score[Student_reg];
		}

		setAveragePreferenceScore(
				Double.valueOf(new DecimalFormat("###.##").format(total_preference_score / total_students)));
	}

	public static void console_output_file() {
		for (int Student_reg = 0; Student_reg < total_students; Student_reg++) {
			System.out.print("Student_" + (Student_reg + 1) + "\t");
			for (int subject = 0; subject < total_subjects; subject++) {
				if (student_matrix[Student_reg][subject].getSubID() != 0) {
					System.out.print(subject_name_list[student_matrix[Student_reg][subject].getSubID() - 1] + "\t");
				}
			}
			System.out.print(student_preference_score[Student_reg] + "\n");
		}
		System.out.println("Average preference_score is: " + getAveragePreferenceScore());
	}

	public static void write_output_file() {
		try {
			File file = new File(getOutput_file_name());
			// if file does not exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("");
			for (int Student_reg = 0; Student_reg < total_students; Student_reg++) {
				bw.write("Student_" + (Student_reg + 1) + "\t");
				for (int subject = 0; subject < total_subjects; subject++) {
					if (student_matrix[Student_reg][subject].getSubID() != 0) {
						bw.write(subject_name_list[student_matrix[Student_reg][subject].getSubID() - 1] + "\t");
					}
				}
				bw.write(student_preference_score[Student_reg] + "\n");
			}
			bw.write("\nAverage preference_score is: " + getAveragePreferenceScore());
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void isAllocationPartial() {
		int student_need_subject = 99;
		for (int Student_reg = 0; Student_reg < total_students; Student_reg++) {
			if (student_EnrollmentCount[Student_reg].getSubjectCount() < 3) {
				student_need_subject = Student_reg;
				int[] sub_possibilities = new int[4 - student_EnrollmentCount[Student_reg].getSubjectCount()];
				int count = 0;
				for (int i = 0; i < total_subjects; i++) {
					if (student_matrix[student_need_subject][i].getPreferenceUsed() == 0) {
						sub_possibilities[count] = i;
						count++;
					}
				}
				student_subject_possibilites.put(student_need_subject, sub_possibilities);
			}
		}

	}

	public static void re_allocation() {

		if (!student_subject_possibilites.isEmpty()) {
			for (int allocation_still_pending_student : student_subject_possibilites.keySet()) {
				int[] subjects_possibilities = student_subject_possibilites.get(allocation_still_pending_student);
				int student_index = 0;
				for (int i = 0; i < subjects_possibilities.length; i++) {
					int sub_pos = subjects_possibilities[i];
					int reshuffle_count = 0;
					if (student_EnrollmentCount[allocation_still_pending_student].getSubjectCount() < 3) {
						for (int st = student_index; reshuffle_count < 2 && st < 12
								&& st != allocation_still_pending_student; st++, reshuffle_count++) {

							int student_reshuffled = st;
							int course_assigned_to_student = sub_pos;
							int reshuffle_course_deallo = sub_pos;
							int reshuffle_course_allo = 99;
							for (int u = 0; u < 4; u++) {
								if (student_matrix[student_reshuffled][u].getPreferenceUsed() == 0) {
									reshuffle_course_allo = u;
								}
							}

							// Course registration - reshuffle
							subject_assignment(student_reshuffled, reshuffle_course_allo);

							// new course assignment
							subject_assignment(allocation_still_pending_student, course_assigned_to_student);

							// Course removing - reshuffle
							subject_removing(student_reshuffled, reshuffle_course_deallo);

							student_index++;
							break;
						}
					}
				}
			}
		}
	}

	public static void subject_assignment(int Student_reg, int subject) {
		int preference_given = student_matrix[Student_reg][subject].getPreferenceGiven();
		int current_enrollment_count = student_EnrollmentCount[Student_reg].getSubjectCount();
		int subject_current_filled_seats = subject_matrix[subject].getFilledSeats();
		student_matrix[Student_reg][subject].setSubID(subject + 1);
		student_matrix[Student_reg][subject].setPreferenceUsed(preference_given);
		student_EnrollmentCount[Student_reg].setSubjectCount(current_enrollment_count + 1);
		subject_matrix[subject].setFilledSeats(subject_current_filled_seats + 1);
	}

	public static void subject_removing(int Student_reg, int subject) {
		int current_enrollment_count = student_EnrollmentCount[Student_reg].getSubjectCount();
		int subject_current_filled_seats = subject_matrix[subject].getFilledSeats();
		student_matrix[Student_reg][subject].setSubID(0);
		student_matrix[Student_reg][subject].setPreferenceUsed(0);
		student_EnrollmentCount[Student_reg].setSubjectCount(current_enrollment_count - 1);
		subject_matrix[subject].setFilledSeats(subject_current_filled_seats - 1);
	}

	public static void first_allocation() {

		for (int preference = 1; preference <= maximum_preference_score; preference++) {
			for (int Student_reg = 0; Student_reg < total_students; Student_reg++) {
				for (int subject = 0; subject < total_subjects; subject++) {
					if (student_EnrollmentCount[Student_reg].getSubjectCount() < 3) {
						if (student_matrix[Student_reg][subject].getPreferenceGiven() == preference) {
							if (subject_matrix[subject].getFilledSeats() < 10
									&& student_EnrollmentCount[Student_reg].getSubjectCount() < 3) {
								subject_assignment(Student_reg, subject);
							}
							break;
						}
					}
				}
			}
		}
	}

	public static void input_file_reading() {
		BufferedReader buffer_reader = null;
		try {
			buffer_reader = new BufferedReader(new FileReader(getPreference_Input_File()));
			String lineJustFetched;
			String[] wordsArray;
			int row_index = 0, column_index = 0;
			Student s;
			Student s1;
			while ((lineJustFetched = buffer_reader.readLine()) != null) {
				wordsArray = lineJustFetched.split(delimiter);
				for (int i = 1; i < wordsArray.length; i++) {
					s = new Student();
					s.setPreferenceGiven(Integer.parseInt(wordsArray[i].toString()));
					s.setMaxEnrollment(total_subject_required_per_student);
					s.setPreferenceUsed(0);
					s.setSubID(0);
					student_matrix[row_index][column_index] = s;

					s1 = new Student();
					s1.setSubjectCount(0);
					student_EnrollmentCount[row_index] = s1;
					column_index++;
				}
				row_index++;
				column_index = 0;
			}

			Subject subject_ref;
			for (int i = 0; i < total_subjects; i++) {
				subject_ref = new Subject();
				subject_ref.setSubID(i + 1);
				subject_ref.setFilledSeats(0);
				subject_ref.setTotalSeats(maximum_seats_per_subject);
				subject_matrix[i] = subject_ref;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Entered input file : " + Preference_Input_File + " is missing.");
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				buffer_reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public static void main(String[] args) {
		
		if(args.length == 0){
			System.out.println("Both arguments are missing");
			System.exit(0);
		}else if(args.length == 1){
			System.out.println("One of the argument is missing");
			System.exit(0);
		}else {
			if(args[0] != null){
				setPreference_Input_File(args[0]);
			}
			if(args[1] != null){
				setOutput_file_name(args[1]);
			}
			
			input_file_reading();
			first_allocation();
			isAllocationPartial();
			re_allocation();
			calculate_preference_score();
			write_output_file();
		}
		
	}
	
	public static String getOutput_file_name() {
		return output_file_name;
	}

	public static void setOutput_file_name(String output_file_name) {
		Driver.output_file_name = output_file_name;
	}
	
	public static double getAveragePreferenceScore() {
		return averagePreferenceScore;
	}

	public static void setAveragePreferenceScore(double averagePreferenceScore) {
		Driver.averagePreferenceScore = averagePreferenceScore;
	}
	
	public static String getPreference_Input_File() {
		return Preference_Input_File;
	}

	public static void setPreference_Input_File(String preference_Input_File) {
		Preference_Input_File = preference_Input_File;
	}

	public static String getDelimiter() {
		return delimiter;
	}

	public static void setDelimiter(String delimiter) {
		Driver.delimiter = delimiter;
	}

	public static int getTotal_students() {
		return total_students;
	}

	public static void setTotal_students(int total_students) {
		Driver.total_students = total_students;
	}

	public static int getTotal_subjects() {
		return total_subjects;
	}

	public static void setTotal_subjects(int total_subjects) {
		Driver.total_subjects = total_subjects;
	}

	public static int getMaximum_preference_score() {
		return maximum_preference_score;
	}

	public static void setMaximum_preference_score(int maximum_preference_score) {
		Driver.maximum_preference_score = maximum_preference_score;
	}

	public static int getMaximum_seats_per_subject() {
		return maximum_seats_per_subject;
	}

	public static void setMaximum_seats_per_subject(int maximum_seats_per_subject) {
		Driver.maximum_seats_per_subject = maximum_seats_per_subject;
	}

	public static int getTotal_subject_required_per_student() {
		return total_subject_required_per_student;
	}

	public static void setTotal_subject_required_per_student(int total_subject_required_per_student) {
		Driver.total_subject_required_per_student = total_subject_required_per_student;
	}

	public static int getReshuffle() {
		return reshuffle;
	}

	public static void setReshuffle(int reshuffle) {
		Driver.reshuffle = reshuffle;
	}

	public static String[] getSubject_name_list() {
		return subject_name_list;
	}

	public static void setSubject_name_list(String[] subject_name_list) {
		Driver.subject_name_list = subject_name_list;
	}

	public static Student[][] getStudent_matrix() {
		return student_matrix;
	}

	public static void setStudent_matrix(Student[][] student_matrix) {
		Driver.student_matrix = student_matrix;
	}

	public static Subject[] getSubject_matrix() {
		return subject_matrix;
	}

	public static void setSubject_matrix(Subject[] subject_matrix) {
		Driver.subject_matrix = subject_matrix;
	}

	public static Student[] getStudent_EnrollmentCount() {
		return student_EnrollmentCount;
	}

	public static void setStudent_EnrollmentCount(Student[] student_EnrollmentCount) {
		Driver.student_EnrollmentCount = student_EnrollmentCount;
	}

	public static int[] getPossible_subjects_required() {
		return possible_subjects_required;
	}

	public static void setPossible_subjects_required(int[] possible_subjects_required) {
		Driver.possible_subjects_required = possible_subjects_required;
	}

	public static int[] getStudents_subject_not_taken() {
		return students_subject_not_taken;
	}

	public static void setStudents_subject_not_taken(int[] students_subject_not_taken) {
		Driver.students_subject_not_taken = students_subject_not_taken;
	}

	public static Map<Integer, int[]> getStudent_subject_possibilites() {
		return student_subject_possibilites;
	}

	public static void setStudent_subject_possibilites(Map<Integer, int[]> student_subject_possibilites) {
		Driver.student_subject_possibilites = student_subject_possibilites;
	}

	public static int[] getStudents_reshuffle_count() {
		return students_reshuffle_count;
	}

	public static void setStudents_reshuffle_count(int[] students_reshuffle_count) {
		Driver.students_reshuffle_count = students_reshuffle_count;
	}

	public static int[] getStudent_preference_score() {
		return student_preference_score;
	}

	public static void setStudent_preference_score(int[] student_preference_score) {
		Driver.student_preference_score = student_preference_score;
	}

}
