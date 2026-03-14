package serie1;

public class Main {

	//PROBLEMA 1 : PUNTAJE DE ARREGLO 
	//Implementar metodo public static int score(int[] numbers)
	
	  public static int score(int[] numbers) {
	        int totalScore = 0;

	        for (int num : numbers) {
	            if (num == 5) {
	                totalScore += 5;
	            } else if (num % 2 == 0) {
	                totalScore += 1;
	            } else {
	                totalScore += 3;
	            }
	        }

	        return totalScore;
	    }
	
	  
	  //Indicaciones 
	  //Complejidad temporal O(n)
	  //Complejidad espacial O(1)
	  
	  //Justifica brevemente su respuesta 
	  //el metodo solo tiene un ciclo for lo que hace cada elemento se visite una vez y no importa si el arreglo tiene 5 o mas 
	  //ya que solo crea y solo utiliza una variable primitiva que esta en memoria 
	  
	  
	  
	  
	  //PROBLEMA 2 :  PUNTAJE DE ARREGLO 
	  //Implementar el metodo public static int[] secondMinMax(int[] numbers)
	  
	  //descripcion: dado un arreglo de enteros, encuentre el segundo menor y el segundo mayor en una sola pasada.
	  
	  //reglas 
	  //debe recorrer el arreglo una sola vez
	  //no se permite ordenar el arreglo 
	  //retornar un arreglo con el formato [segundoMenor, segundoMayor]
	  //Puede asumir que el arreglo tiene al menos 2 elementos distintos 
	  
	  
	  public static int[] secondMinMax(int[] numbers) {
		    // Inicializamos con los valores 
		    int min1 = Integer.MAX_VALUE;
		    int min2 = Integer.MAX_VALUE;
		    int max1 = Integer.MIN_VALUE;
		    int max2 = Integer.MIN_VALUE;

		    for (int num : numbers) {
		        // para los segundos mayores 
		        if (num > max1) {
		            max2 = max1; 
		        } else if (num > max2 && num < max1) {
		            max2 = num;
		        }
		        
		       
		        //para los segundos minimos 
		        if (num < min1) {
		            min2 = min1; 
		            min1 = num;  
		        } else if (num < min2 && num > min1) {
		            min2 = num;
		        }
		    }

		    return new int[]{min2, max2};
		}
	  
	  
	 
	  //Complejidad temporal O(n) solo se recorre el arreglo una vez 
	  //Complejidad espacial O(1)
	  
	  //Justifica brevemente su respuesta 
	  
	  
	  

}

