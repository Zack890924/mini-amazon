// src/types/api.ts
export interface Product {
    id: number;
    worldProductId: number;
    description: string;
    inventory: number;
  }
  
  export interface OrderItemDTO {
    productId: number;
    description?: string;
    quantity: number;
  }
  
  export interface DeliveryAddressDTO {
    x: number;
    y: number;
  }
  
  export interface PurchaseRequestDTO {
    items: OrderItemDTO[];
    deliveryAddress: DeliveryAddressDTO;
    upsAccount?: string;
    packageId?: number;
  }
  
  export interface PurchaseResponseDTO {
    orderId: number;
    status: string;
    message: string;
  }
  
  export interface PackageOperationRequestDTO {
    packageId: number;
    truckId?: number;
    items?: OrderItemDTO[];
  }
  
  export interface PackageOperationResponseDTO {
    packageOperationId: number;
    message: string;
  }
  
  export interface PackageOperation {
    id: number;
    packageId: number;
    operation: string; 
    status: string;
    orderItems?: string;
    truckId?: number;
  }