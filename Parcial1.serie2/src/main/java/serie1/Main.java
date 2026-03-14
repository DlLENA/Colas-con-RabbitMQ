package serie1;

public class Main {

	//PROBLEMA 1 : PUNTAJE DE ARREGLO 
	//Implementar metodo public static int score(int[] numbers)
	
	  public static int score(int[] numbers) {
	        int totalScore = 0;

	        for (int num : numbers) {
	            if (num == 5) {
	                // Rule 3: +5 for every number equal to 5
	                totalScore += 5;
	            } else if (num % 2 == 0) {
	                // Rule 1 & 4: +1 for even numbers (including 0)
	                totalScore += 1;
	            } else {
	                // Rule 2: +3 for odd numbers except 5
	                totalScore += 3;
	            }
	        }

	        return totalScore;
	    }
	

}

