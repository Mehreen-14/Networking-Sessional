import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";

    ////////////////////------starts------/////////////////////////////////
    private static String ChartoBinary(char c) {
        String str = Integer.toBinaryString(c);
        return String.format("%8s", str).replace(' ', '0');
    }

    private static String[] createDataBlock(String paddedData, int m) {
        int rows = paddedData.length() / m;
        String[] dataBlock = new String[rows];

        int startIndex = 0;
        int endIndex = m;

        for (int i = 0; i < rows; i++) {
            String row = paddedData.substring(startIndex, endIndex);
            dataBlock[i] = row.chars().mapToObj(c -> ChartoBinary((char) c)).reduce("", String::concat);
            startIndex = endIndex;
            endIndex += m;
        }

        return dataBlock;
    }

    private static void printDataBlock(String[] dataBlock) {
        for (String row : dataBlock) {
            System.out.println(row);
        }
    }

    private static String[] CheckBits(String[] dataBlock){
        String[] result = new String[dataBlock.length];
        for (int rowIdx = 0; rowIdx < dataBlock.length; rowIdx++){
            String row = dataBlock[rowIdx];
            int r = 0; // Number of check bits

           while (Math.pow(2, r) < row.length() + r + 1) {
                r++;
            }

            //System.out.println("R "+r);
            int[] checkBits = new int[r];
            int[] dataWithCheckBits = new int[row.length() + r];


            for (int i = 0; i < dataWithCheckBits.length; i++) {
                dataWithCheckBits[i] = -1;
            }

            int dataIndex = 0;
            int checkIndex = 0;

            for (int i = 0; i < dataWithCheckBits.length; i++) {
                if ((i + 1) == Math.pow(2, checkIndex)) {
                    checkBits[checkIndex] = 0;
                    dataWithCheckBits[i] = -1;
                    checkIndex++;
                } else {
                    dataWithCheckBits[i] = Integer.parseInt(String.valueOf(row.charAt(dataIndex)));
                    dataIndex++;
                }
            }

            for (int i = 0; i < r; i++) {
                int position = (int) Math.pow(2, i);

                for (int j = position - 1; j < dataWithCheckBits.length; j += 2 * position) {
                    for (int k = j; k < j + position && k < dataWithCheckBits.length; k++) {
                        if (dataWithCheckBits[k] != -1) {
                            //System.out.print(dataWithCheckBits[k]+" ");
                            checkBits[i] ^= dataWithCheckBits[k];
                        }
                    }
                }

                dataWithCheckBits[position - 1] = checkBits[i];
                //System.out.println();
            }


            // Create the result string with data and check bits
            StringBuilder resultString = new StringBuilder();
            int indx = 0;
            for (int i = 0; i < dataWithCheckBits.length; i++) {
                if (dataWithCheckBits[i] != -1) {
                    int p = i+1;
                    if (p == Math.pow(2,indx)) {
                        //check bit will be in green color
                        System.out.print(ANSI_GREEN);
                        System.out.print(dataWithCheckBits[i]);
                        System.out.print(ANSI_RESET);
                        resultString.append(dataWithCheckBits[i]);
                        indx++;
                    } else {
                        System.out.print(dataWithCheckBits[i]);
                        resultString.append(dataWithCheckBits[i]);
                    }

                }
            }
            System.out.println();
            //System.out.println(resultString.toString());
            result[rowIdx] = resultString.toString();
        }

        return result;
    }


    private static String SerializationWithColumnMajor(String[] dataBlock) {
        int numRows = dataBlock.length;
        int numCols = dataBlock[0].length();

        StringBuilder serializedData = new StringBuilder();

        for (int j = 0; j < numCols; j++) {
            for (int i = 0; i < numRows; i++) {
                serializedData.append(dataBlock[i].charAt(j));
            }
        }

        return serializedData.toString();
    }


    private static String CRCMethod(String bitStream, String polynomial) {


        int polynomialLength = polynomial.length() - 1;
        for (int i = 0; i < polynomialLength; i++) {
            bitStream += "0";
        }

        int dataLength = bitStream.length();

        StringBuilder dividend = new StringBuilder(bitStream);
        StringBuilder divisor = new StringBuilder(polynomial);

        while (dataLength >= (polynomialLength+1)) {
             int index = dividend.indexOf("1");
            if (index == -1) {
                break;
            }

            for (int i = 0; i <= polynomialLength; i++) {
                if (index+i < dividend.length()){
                    if (divisor.charAt(i) == dividend.charAt(index + i)) {
                        dividend.setCharAt(index + i, '0');
                         }
                    else {
                        dividend.setCharAt(index + i, '1');

                    }
                }
                else
                    break;
            }
             while (dividend.length() > 1 && dividend.charAt(0) == '0') {
                dividend.deleteCharAt(0);
            }


            dataLength = dividend.length();

        }

        // Left-pad the CRC with '0's to match the polynomial length
        while (dividend.length() < polynomialLength) {
            dividend.insert(0, '0');
        }

        return dividend.toString();
    }

    private static String simulateTransmission(String frame, double prob) {
        StringBuilder receivedFrame = new StringBuilder(frame);
        Random random = new Random();
        for (int i = 0; i < receivedFrame.length(); i++) {
            if (random.nextDouble() < prob) {
                // Toggle
                char originalBit = receivedFrame.charAt(i);
                char toggledBit;
                if (originalBit == '0'){
                    toggledBit = '1';
                }
                else {
                    toggledBit = '0';
                }
                receivedFrame.setCharAt(i, toggledBit);

            }
        }

        return receivedFrame.toString();
    }

    private static String CheckError(String bitstream,String polynomial){
        int dataLength = bitstream.length();
        int polynomialLength = polynomial.length();
        StringBuilder dividend = new StringBuilder(bitstream);
        StringBuilder divisor = new StringBuilder(polynomial);

        while (dataLength >= (polynomialLength)) {
            int index = dividend.indexOf("1");
            if (index == -1) {
                break;
            }

            for (int i = 0; i < polynomialLength; i++) {
                if (index+i < dividend.length()){
                    if (divisor.charAt(i) == dividend.charAt(index + i)) {
                        dividend.setCharAt(index + i, '0');
                    }
                    else {
                        dividend.setCharAt(index + i, '1');

                    }
                }
                else
                    break;
            }
            while (dividend.length() > 1 && dividend.charAt(0) == '0') {
                dividend.deleteCharAt(0);
            }


            dataLength = dividend.length();

        }
        //System.out.println("Data Length "+dataLength);

        return dividend.toString();
    }


    private static String[] DeserializationWithColumnMajor(String input, int numRows, String[] given) {
        int numColumns = input.length() / numRows;

        String[] deserializedData = new String[numRows];

        // Print the data in separate columns
        for (int i = 0; i < numRows; i++) {
            StringBuilder row = new StringBuilder();

            for (int j = 0; j < numColumns; j++) {
                int index = j * numRows + i;
                char inputChar = input.charAt(index);
                char givenChar = given[i].charAt(j);

                if (inputChar == givenChar) {
                    System.out.print(inputChar);
                    row.append(inputChar);
                } else {
                    System.out.print(ANSI_RED + inputChar + ANSI_RESET);
                    row.append(inputChar);
                }
            }
            System.out.println();
            deserializedData[i] = row.toString();
        }
        return deserializedData;
    }

    private static String computeAsciiFromBits(String[] deserializedData) {
        StringBuilder asciiData = new StringBuilder();

        for (String row : deserializedData) {
            for (int i = 0; i < row.length(); i += 8) {
                String eightBits = row.substring(i, Math.min(i + 8, row.length()));
                int asciiValue = Integer.parseInt(eightBits, 2);
                char asciiChar = (char) asciiValue;
                asciiData.append(asciiChar);
            }
        }

        return asciiData.toString();
    }


    private static String[] removeCheckBits(String[] deserialized) {
        String[] correctedDataBlock = new String[deserialized.length];
        for (int i = 0; i < deserialized.length; i++) {
            StringBuilder correctedRow = new StringBuilder();
            int power = 0; // Power of 2 for check bit positions
            for (int j = 0; j < deserialized[i].length(); j++) {
                if (j != (int) Math.pow(2, power) - 1) {
                    correctedRow.append(deserialized[i].charAt(j));
                } else {
                    power++; // Move to the next check bit position
                }
            }
            correctedDataBlock[i] = correctedRow.toString();
        }
        return correctedDataBlock;
    }



    private static String[] detectAndCorrectErrors(String[] deserialized,int m) {
        int r = 0;
        for (int ridx = 0; ridx < deserialized.length; ridx++) {
            String row = deserialized[ridx];
            r = row.length() - (m * 8);
            int errorPosition = 0;

            int[] checkBits = new int[r];
            int[] dataWithCheckBits = new int[row.length()];


            for (int i = 0; i < dataWithCheckBits.length; i++) {
                dataWithCheckBits[i] = -1;
            }

            int checkIndex = 0;

            for (int i = 0; i < dataWithCheckBits.length; i++) {
                if ((i + 1) == Math.pow(2, checkIndex)) {
                    checkBits[checkIndex] = 0;
                    dataWithCheckBits[i] = -1;
                    checkIndex++;
                } else {
                    dataWithCheckBits[i] = Integer.parseInt(String.valueOf(row.charAt(i)));
                }
            }


            for (int i = 0; i < r; i++) {
                int position = (int) Math.pow(2, i);

                for (int j = position - 1; j < dataWithCheckBits.length; j += 2 * position) {
                    for (int k = j; k < j + position && k < dataWithCheckBits.length; k++) {
                        if (dataWithCheckBits[k] != -1) {
                            //System.out.print(dataWithCheckBits[k]+" ");
                            checkBits[i] ^= dataWithCheckBits[k];
                        }
                    }
                }
                dataWithCheckBits[position - 1] = checkBits[i];
                //System.out.println();
                //System.out.println("Row[position-1] "+row.charAt(position-1)+" checkBits[i]: "+dataWithCheckBits[position-1]);

               // if ((int)row.charAt(position-1) != Character.forDigit(checkBits[i], 10))
                if (row.charAt(position-1) != Character.forDigit(dataWithCheckBits[position-1], 10)){
                    //System.out.println("I'm here "+(position));
                    errorPosition+=(position);
                }



            }
//            for (int i=0;i<r;i++){
//                System.out.print(checkBits[i]);
//            }
            //System.out.println();
//            String pos = intArrayToStringReverse(checkBits);
//            System.out.println("Maliha position "+pos);
//            int decimalNumber = Integer.parseInt(pos, 2);
//            System.out.println("Maliha decimal  "+decimalNumber);
//
            //System.out.println("here "+row);
             char[] charArray = row.toCharArray();
            errorPosition = errorPosition-1;
            //System.out.println("Error at "+errorPosition);
            if (errorPosition > 0 && errorPosition < row.length()){
                //System.out.println("Flipped");
                if (charArray[errorPosition] == '0'){
                    charArray[errorPosition] = '1';
                }
                else if (charArray[errorPosition] == '1'){
                    charArray[errorPosition] = '0';
                }
            }

            row = new String(charArray);
            deserialized[ridx] = row;
            //System.out.println("there "+row);
        }

        return deserialized;
    }

    /////////////////-----ends-----//////////////////////////////


    public static void main(String[] args) {
        Scanner sc= new Scanner(System.in);
        System.out.print("enter data string: ");
        String input = sc.nextLine();
        System.out.print("enter number of data bytes in a row (m): ");
        int m = sc.nextInt();
        System.out.print("enter probability (p): ");
        double prob = sc.nextDouble();
        System.out.print("enter generator polynomial: ");
        String polynomial = sc.next();

        //System.out.println(input.length());
        int new_size = -1;
        StringBuilder changedData = new StringBuilder(input);

        if ((input.length() % m) != 0) {
            new_size = m - ((input.length()) % m);
            for (int i=0;i<new_size;i++){
                changedData.append("~");
            }
        }

        System.out.println();
        //Task 1
        System.out.println("data string after padding: "+changedData.toString());
        System.out.println();

        //Task 2
        String[] dataBlock = createDataBlock(changedData.toString(), m);
        System.out.println("data block (ascii codes of m characters per row):");
        printDataBlock(dataBlock);
        System.out.println();

        //Task 3
        System.out.println("data block after adding check bits:");
        String[] result = CheckBits(dataBlock);
        int numRows = result.length;
        System.out.println();

        //Task 4
        System.out.println("data bits after column-wise serialization:");
        String serializedData = SerializationWithColumnMajor(result);
        System.out.println(serializedData);
        System.out.println();

        //Task 5
        System.out.println("data bits after appending CRC checksum (sent frame):");
        String frame = serializedData + ANSI_CYAN + CRCMethod(serializedData, polynomial) + ANSI_RESET;
        System.out.println(frame);
        System.out.println();

        //Task 6
        System.out.println("received frame:");
        frame = serializedData+CRCMethod(serializedData,polynomial);

        String received = simulateTransmission(frame, prob);
        for (int i = 0; i < received.length(); i++) {
            char bit = received.charAt(i);
            if (bit == frame.charAt(i)) {
                System.out.print(ANSI_RESET + bit);
            } else {
                System.out.print(ANSI_RED + bit + ANSI_RESET);
            }
        }

        System.out.println();
        System.out.println();

        //Task 7
        System.out.print("result of CRC checksum matching: ");
        String res = CheckError(received,polynomial);
        boolean isZero = res.chars().allMatch(c -> c == '0');
        if (isZero) {
            System.out.println("no error detected");
        } else {
            System.out.println("error detected");
        }

        System.out.println();
        //Task 8
        System.out.println("data blocks after removing CRC checksum bits:");
        String Data_removingCRC = received.substring(0, received.length() - polynomial.length() + 1);
        String[] deserialized = DeserializationWithColumnMajor(Data_removingCRC,numRows,result);

        System.out.println();


        //Task 9
        String[] corrected = detectAndCorrectErrors(deserialized,m);
        //printDataBlock(corrected);

        System.out.println("data block after removing check bits:");
        String[] withoutcheckbitDataBlock = removeCheckBits(corrected);
        printDataBlock(withoutcheckbitDataBlock);
        System.out.println();


        //Task 10
        String asciiDataString = computeAsciiFromBits(withoutcheckbitDataBlock);
        System.out.println("output frame: " + asciiDataString);
    }


}

/*
If the sum of the positions where the check bits differ from their expected values (i.e., the error positions)
is greater than the length of the received string, it implies that there are more errors detected than there are
bits in the received string. This scenario is unusual and could indicate a severe data corruption issue.
 */