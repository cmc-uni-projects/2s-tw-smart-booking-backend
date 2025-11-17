import mysql.connector
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document

# --- CẤU HÌNH DATABASE ---
db_config = {
    'user': 'root',
    'password': 'Qthanh0704@@',  # Mật khẩu của bạn
    'host': 'localhost',
    'database': 'smart_booking_db', # Tên DB chuẩn
    'charset': 'utf8mb4'            # Hỗ trợ tiếng Việt không lỗi font
}

def load_and_index_data():
    print("🔄 Đang kết nối Database để học dữ liệu...")
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        # 1. Lấy danh sách Khách sạn đang hoạt động
        query_props = """
            SELECT propertyId, propertyName, description, address, city 
            FROM properties 
            WHERE isActive = 1
        """
        cursor.execute(query_props)
        properties = cursor.fetchall()
        
        if not properties:
            print("⚠️ Database đang trống hoặc không có khách sạn nào active!")
            return

        documents = []
        print(f"✅ Tìm thấy {len(properties)} khách sạn. Đang lấy thêm thông tin phòng...")

        for prop in properties:
            p_id = prop['propertyId']
            
            # 2. Lấy thông tin Phòng (Rooms) của khách sạn này
            # Table 'rooms' dựa trên entity Room.java của bạn
            query_rooms = f"""
                SELECT roomName, pricePerNight, description 
                FROM rooms 
                WHERE propertyId = {p_id} AND isActive = 1
            """
            cursor.execute(query_rooms)
            rooms = cursor.fetchall()

            # Tạo đoạn văn mô tả các phòng
            # Ví dụ: "Phòng Deluxe (500.000 VND) - View biển"
            room_text_list = []
            for r in rooms:
                price = "{:,.0f}".format(r['pricePerNight']).replace(",", ".") 
                r_desc = r['description'] or ""
                room_text_list.append(f"{r['roomName']} (Giá: {price} VND) - {r_desc}")
            
            room_info_str = "; ".join(room_text_list) if room_text_list else "Hiện chưa có thông tin phòng cụ thể."

            # 3. Xử lý dữ liệu Null & Làm sạch
            name = prop['propertyName'] or ""
            desc = prop['description'] or ""
            addr = prop['address'] or ""
            
            # Cắt khoảng trắng thừa ở tên thành phố (Quan trọng để lọc đúng)
            raw_city = prop['city'] or ""
            city = raw_city.strip() 

            # 4. GOM TẤT CẢ VÀO NỘI DUNG HỌC (Full Context)
            full_content = (
                f"Tên khách sạn: {name}. "
                f"Địa điểm: {addr}, {city}. "
                f"Mô tả: {desc}. "
                f"Chi tiết phòng: {room_info_str}."
            )
            
            # 5. Gắn nhãn metadata (Để sau này lọc)
            meta = {
                "property_id": p_id,
                "city": city 
            }
            
            doc = Document(page_content=full_content, metadata=meta)
            documents.append(doc)

        print(f"✅ Đã tạo dữ liệu chi tiết cho {len(documents)} khách sạn. Đang tạo Vector...")

        # Tạo Vector DB
        embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
        vector_db = FAISS.from_documents(documents, embeddings)

        # Lưu xuống ổ cứng
        vector_db.save_local("vector_store")
        print("🎉 Xong! Dữ liệu AI (Khách sạn + Phòng) đã được cập nhật thành công.")

    except Exception as e:
        print(f"❌ Lỗi Indexer: {e}")
    finally:
        if conn and conn.is_connected():
            conn.close()

if __name__ == "__main__":
    load_and_index_data()