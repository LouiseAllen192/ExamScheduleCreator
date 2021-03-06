import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class is14175819 {
    public static void main(String[] args) throws IOException {
        ExamScheduleCreator run = new ExamScheduleCreator();
        run.start();
    }

}

class ExamScheduleCreator {

    HashMap<String, Integer> inputs;
    ArrayList<HashSet<Integer>> studentModuleInfo;
    ArrayList<int[][]> initialOrderings;
    ArrayList<int[][]> newOrderings = null;
    ArrayList<int[][]> previousOrderings = null;
    int requestedPopulationSize;
    boolean populationSizeChanged;

    public ExamScheduleCreator() throws IOException {
        //inputs = requestInputs();

        //TODO: remove before submission
        inputs = new HashMap<>();
        inputs.put("G", 150);
        inputs.put("P", 30);
        inputs.put("S", 80);
        inputs.put("M", 36);
        inputs.put("C", 5);
        inputs.put("D", 18);
        inputs.put("re", 80);
        inputs.put("mu", 10);
        inputs.put("cr", 10);

    }

    public void start() throws IOException {
        if (inputs != null) {

            studentModuleInfo = generateStudentModuleInfo(inputs.get("S"), inputs.get("C"), inputs.get("M"));
            printStudentModuleInfo(studentModuleInfo);

            requestedPopulationSize = inputs.get("P");
            populationSizeChanged = adjustPopulationSizeRelativeToTotalNumberOfModules(inputs, requestedPopulationSize);

            initialOrderings = generateOrderings(inputs, studentModuleInfo);

            previousOrderings = initialOrderings;
            sortOrderingsOnFitnessCost(previousOrderings);

            for (int i = 0; i < inputs.get("G"); i++) {
                performSelection(previousOrderings);
                previousOrderings = applyGeneticAlgorithm(previousOrderings, inputs, studentModuleInfo);
                recalculateFitnessCostOfOrderings(previousOrderings, studentModuleInfo);

                sortOrderingsOnFitnessCost(previousOrderings);

                addNewLineToFile("\n\n");
                printOrdering(previousOrderings.get(0), i);
                addNewLineToFile("\n");


            }

            if (populationSizeChanged) {
                printWarningForAdjustedPopulationSize(requestedPopulationSize, inputs.get("P"), inputs.get("M"));
            }
        }
    }

    private  HashMap<String, Integer> requestInputs() {
        HashMap<String, Integer> hmap = new HashMap<String,Integer>();
        Scanner scan = new Scanner(System.in);
        boolean quit = false;

        UserInput[] ins = {
                new UserInput("number of generations", "G", 0, scan),
                new UserInput("population size", "P", 1, scan),
                new UserInput("number of students", "S", 2, scan),
                new UserInput("total number of modules", "M", 3, scan),
                new UserInput("the number of modules per course", "C", 4, scan),
                new UserInput("the percentage chance of Reproduction", "re", 5, scan),
                new UserInput("the percentage chance of Mutation", "mu", 6, scan)
        };

        for (int i = 0; i < ins.length && !quit; i++) {
            ins[i].getUserInput();
            if (ins[i].getValue() == -1) {
                quit = true;
                hmap = null;
            } else {
                hmap.put(ins[i].getKey(), ins[i].getValue());
            }
        }

        if (hmap != null) {
            int numExamDays = hmap.get("M") / 2;
            hmap.put("D", numExamDays);

            int percentageChanceOfCrossover = (100 - (hmap.get("mu") + hmap.get("re")));
            hmap.put("cr", percentageChanceOfCrossover);
        }

        return hmap;
    }

    private boolean adjustPopulationSizeRelativeToTotalNumberOfModules(HashMap<String, Integer> inputs, int requestedPopulationSize) {
        //For M total number of modules there are a limited amount of unique permutations.
        // We will limit the population size P to the factorial of M (less than 800)
        int  maxPopulationSize = getMaxPopulationSize(inputs.get("M"));

        if (requestedPopulationSize > maxPopulationSize) {
            inputs.put("P", maxPopulationSize);
            return true;
        } else {
            return false;
        }
    }

    private int getMaxPopulationSize(int numModules) {
        final int MAX_PERMISSABLE_POPULATION_SIZE = 800;
        int maxPopulationSize = 1;
        for (int i = 1; i <= numModules; i++) {
            maxPopulationSize = (maxPopulationSize * i);
            if (maxPopulationSize < 0) {
                break;
            }
        }

        if (maxPopulationSize > MAX_PERMISSABLE_POPULATION_SIZE || maxPopulationSize <= 0) {
            maxPopulationSize = MAX_PERMISSABLE_POPULATION_SIZE;
        }
        return maxPopulationSize;
    }

    private ArrayList<HashSet<Integer>> generateStudentModuleInfo(int numStudents, int numModulesPerCourse, int totalNumberOfModules) {
        ArrayList<HashSet<Integer>> studentModuleInfo = new ArrayList<>();
        for (int i = 0; i < numStudents; i++) {
            HashSet<Integer> studentModules = new HashSet<>();
            for (int j = 0; j < numModulesPerCourse; j++) {
                boolean valid = false;
                while(!valid) {
                    int rand = getRandomNumberInRange(totalNumberOfModules, 1);
                    if (!studentModules.contains(rand)) {
                        valid = true;
                        studentModules.add(rand);
                    }
                }
            }
            studentModuleInfo.add(studentModules);
        }
        return studentModuleInfo;
    }

    private ArrayList<int[][]> generateOrderings(HashMap<String, Integer> inputs, ArrayList<HashSet<Integer>> studentModuleInfo) {
        ArrayList <int[][]> orderings = new ArrayList<>();
        for (int i = 0; i < inputs.get("P"); i++) {
            boolean valid = false;
            while(!valid) {
                int[][] ordering = createOrdering(inputs.get("D"), inputs.get("M"), studentModuleInfo);
                if (!isOrderingAlreadyInOrderingsList(ordering, orderings)) {
                    orderings.add(ordering);
                    valid = true;
                }
            }
        }
        return orderings;
    }

    private int[][] createOrdering(int numberOfExamDays, int totalNumberOfModules, ArrayList<HashSet<Integer>> studentModuleInfo) {
        int NUMBER_EXAMS_PER_DAY = 2;
        int fitnessCost = 0;
        int[][] ordering = new int[numberOfExamDays + 1][NUMBER_EXAMS_PER_DAY];
        HashSet<Integer> alreadyScheduledExams = new HashSet<>();

        for (int i = 0; i < numberOfExamDays; i++) {
            for (int j = 0; j < NUMBER_EXAMS_PER_DAY; j++) {
                boolean valid = false;
                while (!valid && alreadyScheduledExams.size() <= totalNumberOfModules) {
                    int rand = getRandomNumberInRange(totalNumberOfModules, 1);
                    if (!alreadyScheduledExams.contains(rand)) {
                        alreadyScheduledExams.add(rand);
                        ordering[i][j] = rand;
                        valid = true;
                    }
                }
            }

            //Calculate fitness cost for ordering while creating it
            int x = calculateFitnessCostForOneExamSession(studentModuleInfo, ordering[i][0], ordering[i][1]);
            fitnessCost+= x;
        }

        ordering[numberOfExamDays][0] = fitnessCost;
        ordering[numberOfExamDays][1] = 0;

        return ordering;
    }

    private boolean isOrderingAlreadyInOrderingsList(int[][] ordering, ArrayList<int[][]> orderings) {
        for (int i = 0; i < orderings.size(); i++) {
            if (Arrays.deepEquals(orderings.get(i), ordering)) {
                return true;
            }
        }
        return false;
    }

    private void recalculateFitnessCostOfOrderings(ArrayList<int[][]> orderings, ArrayList<HashSet<Integer>> studentModuleInfo) {
        for (int i = 0; i < orderings.size(); i++) {
            fitnessFunction(orderings.get(i), studentModuleInfo);
        }
    }

    private void fitnessFunction(int[][] ordering, ArrayList<HashSet<Integer>> studentModuleInfo) {
        int cost = 0;
        for (int i = 0; i < ordering.length; i++) {
            int session1 = ordering[i][0];
            int session2 = ordering[i][1];

            cost += calculateFitnessCostForOneExamSession(studentModuleInfo, session1, session2);
        }

        ordering[ordering.length - 1][0] = cost;
    }

    private int calculateFitnessCostForOneExamSession(ArrayList<HashSet<Integer>> studentModuleInfo, int exam1, int exam2) {
        int cost = 0;
        for (int studentId = 0; studentId < studentModuleInfo.size(); studentId++) {
            if(studentTakesModule(studentId, exam1, studentModuleInfo) && studentTakesModule(studentId, exam2, studentModuleInfo)) {
                cost++;
            }
        }
        return cost;
    }

    private boolean studentTakesModule(int studentID, int moduleNumber, ArrayList<HashSet<Integer>> studentModuleInfo) {
        return studentModuleInfo.get(studentID).contains(moduleNumber);
    }

    private void performSelection(ArrayList<int[][]> orderings) {
        if (orderings.size() > 3) {
            int mod = orderings.size() % 3;
            int divisionNumber = (int) Math.floor((orderings.size() + mod) / 3);

            int startingIndex = orderings.size() - divisionNumber;
            for (int i = 0; i < divisionNumber; i++) {
                orderings.set(startingIndex, copyMultiDimArray(orderings.get(i)));
                startingIndex++;
            }
        }
    }

    private int[][] copyMultiDimArray(int[][] old) {
        int numExamDays = old.length - 1;
        int[][] copy = new int[old.length][2];

        for(int i = 0; i < old.length - 1; i++) {
            copy[i][0] = old[i][0];
            copy[i][1] = old[i][1];
        }

        copy[numExamDays][0] = old[numExamDays][0];
        copy[numExamDays][1] = 0;

        return copy;
    }

    private void sortOrderingsOnFitnessCost(ArrayList<int[][]> initialOrderings) {
        Collections.sort(initialOrderings, new Comparator<int[][]>() {
            @Override
            public int compare(int[][] o1, int[][] o2) {
                int fitnessCost1 = o1[o1.length - 1][0];
                int fitnessCost2 = o2[o2.length - 1][0];

                return (fitnessCost1 > fitnessCost2) ? 1 : (fitnessCost1 == fitnessCost2) ? 0 : -1;
            }
        });
    }

    private enum Technique {
       REPRODUCTION, CROSSOVER, MUTATION
    }

    private ArrayList<int[][]> applyGeneticAlgorithm(ArrayList<int[][]> previousOrderings, HashMap<String, Integer> inputs, ArrayList<HashSet<Integer>> studentModuleInfo) throws IOException {
        ArrayList<int[][]> orderingsPostGenAlg = new ArrayList<>();

        while (previousOrderings.size() > 0) {
            Technique technique = chooseTechnique(inputs.get("re"), inputs.get("mu"), inputs.get("cr"));
            int[][] newOrdering = null;
            int[][] newOrdering2 = null;

            int[] randomIndexes = getRandomIndexes(previousOrderings.size() -1);
            int randomIndexOfOrdering1 = randomIndexes[0];
            int randomIndexOfOrdering2 = randomIndexes[1];

            //System.out.print("\ntechnique: " + technique + "     ");
            //System.out.print("index: " + randomIndexOfOrdering1 + "  /  ");

            switch (technique) {
                case CROSSOVER:
                    if (previousOrderings.size() != 1 && isOrderingLargeEnoughForCrossover(previousOrderings.get(0))) {
                        //System.out.println("index2: " + randomIndexOfOrdering2);

                        ArrayList<int[][]> postCrossoverOrderings = applyCrossover(previousOrderings.get(randomIndexOfOrdering1), previousOrderings.get(randomIndexOfOrdering2));
                        newOrdering = postCrossoverOrderings.get(0);
                        newOrdering2 = postCrossoverOrderings.get(1);

                        fitnessFunction(newOrdering, studentModuleInfo);
                        fitnessFunction(newOrdering2, studentModuleInfo);
                        orderingsPostGenAlg.add(newOrdering);
                        orderingsPostGenAlg.add(newOrdering2);
                        if (randomIndexOfOrdering1 > randomIndexOfOrdering2) {
                            previousOrderings.remove(randomIndexOfOrdering1);
                            previousOrderings.remove(randomIndexOfOrdering2);
                        } else {
                            previousOrderings.remove(randomIndexOfOrdering2);
                            previousOrderings.remove(randomIndexOfOrdering1);
                        }
                    }
                    break;
                case MUTATION:
                    newOrdering  = applyMutation(previousOrderings.get(randomIndexOfOrdering1));
                    fitnessFunction(newOrdering, studentModuleInfo);
                    orderingsPostGenAlg.add(newOrdering);
                    previousOrderings.remove(randomIndexOfOrdering1);
                    break;
                case REPRODUCTION:
                    newOrdering = previousOrderings.get(randomIndexOfOrdering1);
                    fitnessFunction(newOrdering, studentModuleInfo);
                    orderingsPostGenAlg.add(newOrdering);
                    previousOrderings.remove(randomIndexOfOrdering1);
                    break;
            }


            /*addNewLineToFile("Previous:\n");
            printOrderings(previousOrderings);
            addNewLineToFile("\n\n");
            addNewLineToFile("New:\n");
            printOrderings(orderingsPostGenAlg);
            addNewLineToFile("\n\n");
            addNewLineToFile("\n\n\n\n");*/
        }

        return orderingsPostGenAlg;
    }

    private boolean isOrderingLargeEnoughForCrossover(int[][] ordering) {
        int numRows = ordering.length - 1;

        // Crossover can only be performed if the ordering has a length of 5 or greater
        return (numRows >= 4);
    }

    private ArrayList<int[][]> applyCrossover(int[][] o1, int[][] o2) {
        boolean left;
        ArrayList<int[][]> postCrossoverOrdering;
        int numDays, numModules, randomCuttingPoint;
        HashMap<Integer, Integer> swapNums1;
        HashMap<Integer, Integer> swapNums2;

        left = true;
        postCrossoverOrdering = new ArrayList();
        numDays = o1.length - 1;
        numModules = numDays * 2;
        randomCuttingPoint = getRandomNumberInRange(numModules - 2, 2);
        //System.out.println(randomCuttingPoint);
        swapNums1 = new HashMap<>();
        swapNums2 = new HashMap<>();

        if (randomCuttingPoint >= numDays) {
            left = false;
            randomCuttingPoint -= numDays;
        }

        if (left) {
            for (int i = 0; i < randomCuttingPoint; i++) {
                swapNums1.put(o1[i][0], i);
                swapNums2.put(o2[i][0], i);
                swapElements(o1, o2, i, 0);
            }
        } else {
            for (int i = randomCuttingPoint; i < numDays; i++) {
                swapNums1.put(o1[i][1], i);
                swapNums2.put(o2[i][1], i);
                swapElements(o1, o2, i, 1);
            }
        }

        removeDuplicates(o1, o2, swapNums1, swapNums2, left);

        postCrossoverOrdering.add(o1);
        postCrossoverOrdering.add(o2);
        return postCrossoverOrdering;
    }

    private void removeDuplicates(int[][] o1, int[][] o2, HashMap<Integer, Integer> swap1, HashMap<Integer, Integer> swap2, boolean left) {
        ArrayList<Integer> duplicateIndexes1 = new ArrayList<>();
        ArrayList<Integer> duplicateIndexes2 = new ArrayList<>();
        int colNum;
        colNum = (left) ? 0 : 1;
        createDuplicateSets(duplicateIndexes1, duplicateIndexes2, swap1, swap2);

        if (duplicateIndexes1.size() > 0) {
            //duplicateIndexes1 and duplicateIndexes2 will always be the same size
            for (int i = 0; i < duplicateIndexes1.size(); i++) {
                int rowNum1 = duplicateIndexes1.get(i);
                int rowNum2 = duplicateIndexes2.get(i);

                int temp = o2[rowNum1][colNum];
                o2[rowNum1][colNum] = o1[rowNum2][colNum];
                o1[rowNum2][colNum] = temp;
            }
        }

    }

    private void createDuplicateSets(ArrayList<Integer> dups1, ArrayList<Integer> dups2, HashMap<Integer, Integer> swap1, HashMap<Integer, Integer> swap2) {
        //add rowNumber of duplicate to two duplicate arrayLists. These will then be swapped after
        for (Map.Entry<Integer, Integer> entry : swap1.entrySet()) {
            if (swap2.get(entry.getKey()) == null) {
                dups1.add(entry.getValue());
            }
        }
        for (Map.Entry<Integer, Integer> entry : swap2.entrySet()) {
            if (swap1.get(entry.getKey()) == null) {
                dups2.add(entry.getValue());
            }
        }
    }

    private void swapElements(int[][] o1, int[][] o2, int index, int colNum) {
        int temp = o1[index][colNum];
        o1[index][colNum] = o2[index][colNum];
        o2[index][colNum] = temp;
    }

    private int[][] applyMutation(int[][] ordering) {
        int numRows = ordering.length - 1;
        int numCols = ordering[0].length;

        int randomRow1 = getRandomNumberInRange(numRows - 1,0);
        int randomCol1 = getRandomNumberInRange(numCols - 1,0);

        int[] randomRowAndCol = getSecondUniqueRandomRowAndCol(randomRow1, randomCol1, numRows, numCols);
        int randomRow2 = randomRowAndCol[0];
        int randomCol2 = randomRowAndCol[1];

        int temp = ordering[randomRow1][randomCol1];
        ordering[randomRow1][randomCol1] = ordering[randomRow2][randomCol2];
        ordering[randomRow2][randomCol2] = temp;

       // System.out.println("Swapping [" + randomRow1 + "," + randomCol1 + "] with [" + randomRow2 + "," + randomCol2 + "]");

        return ordering;
    }

    private int[] getSecondUniqueRandomRowAndCol(int r1, int c1, int numRows, int numCols) {
        int[] rowAndCol = new int[2];
        int randomRow2, randomCol2;
        boolean valid = false;

        randomRow2 = getRandomNumberInRange(numRows - 1,0);
        randomCol2 = 0;

        if (randomRow2 == r1) {
            while(!valid) {
                randomCol2 = getRandomNumberInRange(numCols - 1,0);
                if (c1 != randomCol2) {
                    valid = true;
                }
            }
        } else {
            randomCol2 = getRandomNumberInRange(numCols - 1,0);
        }

        rowAndCol[0] = randomRow2;
        rowAndCol[1] = randomCol2;

        return rowAndCol;
    }

    private int[] getRandomIndexes(int max) {
        int[] randIndexes = new int[2];
        boolean valid = false;
        int randomIndexOfOrdering_1, randomIndexOfOrdering_2;

        if (max == 0) {
            randIndexes[0] = 0;
            randIndexes[1] = 0;
        } else if (max == 1) {
            randIndexes[0] = 0;
            randIndexes[1] = 1;
        } else {
            randomIndexOfOrdering_1 = getRandomNumberInRange(max, 0);
            randomIndexOfOrdering_2 = 0;

            while (!valid) {
                //Will always be one less than max. After we remove the first element the indexes will shrink by 1
                randomIndexOfOrdering_2 = getRandomNumberInRange(max - 1, 0);

                if (randomIndexOfOrdering_1 != randomIndexOfOrdering_2) {
                    valid = true;
                }
            }

            randIndexes[0] = randomIndexOfOrdering_1;
            randIndexes[1] = randomIndexOfOrdering_2;
        }

        return randIndexes;
    }

    private Technique chooseTechnique(int reproduction, int mutation, int crossover) {
        int randomNumber = getRandomNumberInRange(100, 1);
        if (randomNumber < reproduction) {
            return Technique.REPRODUCTION;
        } else if (randomNumber < crossover +  reproduction) {
            return Technique.CROSSOVER;
        } else {
            return Technique.MUTATION;
        }
    }

    private int getRandomNumberInRange(int max, int min) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void printStudentModuleInfo(ArrayList<HashSet<Integer>> arr) throws IOException {
        FileWriter fw = new FileWriter("AI17.txt", false);
        for (int i = 0 ; i < arr.size(); i++) {
            fw.write("Student " + (i + 1) + ": ");

            Iterator<Integer> it = arr.get(i).iterator();
            while(it.hasNext()){
                fw.write("M" + it.next() + " ");
            }
            fw.write("\n");

        }
        fw.close();
    }

    private void printOrderings(ArrayList<int[][]> orderings) throws IOException{
        for (int i = 0; i < orderings.size(); i++) {
            int[][] ordering = orderings.get(i);
            printOrdering(ordering, i);
        }
    }

    private void printOrdering(int[][] ordering, int index) throws IOException{
        FileWriter fw = new FileWriter("AI17.txt", true);
        fw.write("\nOrd" + (index + 1) + ":\t");
        for (int i = 0; i < ordering.length - 1; i++) {
            String str = String.format("M%-3d  ", ordering[i][0]);
            fw.write(str);
        }

        fw.write(": cost: " + ordering[ordering.length-1][0] + "\n\t\t");

        for (int i = 0; i < ordering.length - 1; i++) {
            String str = String.format("M%-3d  ", ordering[i][1]);
            fw.write(str);
        }
        fw.write("\n");
        fw.close();
    }

    //delete before submission!!!!!!!!!!!!!!!!!!!
    private void addNewLineToFile(String line)  throws IOException {
        FileWriter fw = new FileWriter("AI17.txt", true);
        fw.write(line);
        fw.close();
    }

    private void printHashMap (HashMap<String, Integer> hmap) {
        for (String name: hmap.keySet()){
            int value = hmap.get(name);
            System.out.println(name + " " + value);
        }
    }

    private void printWarningForAdjustedPopulationSize(int original, int max, int totalNumOfModules) throws IOException{
        FileWriter fw = new FileWriter("AI17.txt", true);
        String warningMsg = "\n\n" +
                "User Requested a Population size of " + original + "\n" +
                "Given that the total number of modules is "+ totalNumOfModules + ", it is not possible to have a population size this large\n" +
                "The maximum permissible number of orderings for " + totalNumOfModules + " modules is: " + max + "\n";
        fw.write(warningMsg);
        fw.close();
    }

}

class UserInput {
    private String prompt;
    private String key;
    private int id;
    private Scanner scan;

    private static int numModulesTotal;
    private static int percentageChanceOfReproduction;

    int value;

    public UserInput(String prompt, String key, int id, Scanner scan) {
        this.prompt = prompt;
        this.key = key;
        this.id = id;
        this.scan = scan;
    }

    public String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }

    public void getUserInput() {
        String input;
        boolean exitLoop = false;

        while(!exitLoop) {
            if (id == 6) {
                System.out.println("NOTE: you have already requested " + UserInput.percentageChanceOfReproduction + "% chance of reproduction." +
                                    "The remaining percentage is to be divided between Mutation and Crossover.");
            }

            System.out.println("Please enter the " + prompt);
            input = scan.nextLine();

            if (isInteger(input)) {
                int number = Integer.parseInt(input);

                String errorMessage = getErrorMsg(number);
                if(errorMessage.equals("")) {
                    exitLoop = true;
                    value = number;

                    if (id == 3) {
                        UserInput.numModulesTotal = number;
                    }
                    if (id == 5) {
                        UserInput.percentageChanceOfReproduction = number;
                    }
                    if (id == 6) {
                        int crossOverPercentage = (100 - (number + UserInput.percentageChanceOfReproduction));
                        //todo
                    }
                } else {
                    System.out.println(errorMessage);
                }

            } else if (input == null) {
                exitLoop = true;
                value = -1;
            } else {
                System.out.println("Invalid input. Please enter an integer\n");
            }
        }
    }

    private String getErrorMsg(int number) {
        if (number == 0) {
            return "Invalid Input. Must be greater than 0\n";
        } else if((id == 3) && !isEven(number)) {
            return "Invalid Input. 'Total number of modules' must be even\n";
        } else if((id == 4) && (number > UserInput.numModulesTotal)) {
            return "Invalid Input. Number of modules per course must be less than the number of total modules (" +  UserInput.numModulesTotal + ")\n";
        } else if ((id == 5) && (number > 100)) {
            return "Invalid input. Number must be between 1-100";
        } else if ((id == 6) && ((number + UserInput.percentageChanceOfReproduction) > 100)) {
            int upperBound = (100 - UserInput.percentageChanceOfReproduction);
            return "Invalid input. You have already requested " +
                    UserInput.percentageChanceOfReproduction + "% chance of reproduction." +
                    "Percentage chance of mutation must be in the range 0 to " + upperBound +
                    "\nRemaining percentage will go to Crossover";
        } else {
            return "";
        }
    }

    private boolean isInteger(String s) {
        try {
            int x = Integer.parseInt(s);
            if (x < 0) {
                return false;
            }
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        return true;
    }

    private boolean isEven(int num) {
        return (num % 2 == 0);
    }
}