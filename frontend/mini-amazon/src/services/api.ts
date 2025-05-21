// src/services/apiService.ts
import { 
    Product, 
    OrderItemDTO, 
    PurchaseRequestDTO, 
    PurchaseResponseDTO, 
    PackageOperation 
  } from '../types/dto';
  
  const API_BASE_URL = 'http://localhost:8080';
  

  export const getAllProducts = async (): Promise<Product[]> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/warehouse/products`);
      if(!response.ok){
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error){
      console.error('Error fetching products:', error);
      throw error;
    }
  };
  
  export const getProductDetails = async (worldProductId: number): Promise<Product> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/warehouse/products/${worldProductId}`);
      if(!response.ok){
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error){
      console.error(`Error fetching product ${worldProductId}:`, error);
      throw error;
    }
  };
  

  export const createOrder = async (
    products: { id: number; name: string; price: string; quantity: number }[],
    coordinates: { x: string; y: string },
    upsAccount?: string
  ): Promise<PurchaseResponseDTO> => {
    try{
      const items: OrderItemDTO[] = products.map(p => ({
        productId: p.id,
        description: p.name,
        quantity: p.quantity
      }));
  
      const purchaseData: PurchaseRequestDTO = {
        items,
        deliveryAddress: {
          x: parseInt(coordinates.x),
          y: parseInt(coordinates.y)
        },
        upsAccount
      };
  
      const response = await fetch(`${API_BASE_URL}/api/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(purchaseData),
      });
      
      if(!response.ok){
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      return await response.json();
    } catch (error){
      console.error('Error creating order:', error);
      throw error;
    }
  };
  

  export const getPackageOperations = async (packageId: number): Promise<PackageOperation[]> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/warehouse/operations/${packageId}`);
      
      if(!response.ok){
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      return await response.json();
    } catch (error){
      console.error('Error fetching package operations:', error);
      throw error;
    }
  };