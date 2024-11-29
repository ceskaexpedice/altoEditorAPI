import argparse
import json
import os
import posixpath
import sys
import time
import tempfile
from PIL import Image

import requests
from requests_toolbelt import MultipartEncoder

SERVER_URL = "https://pero-ocr.fit.vutbr.cz/api/"
pero_temp_path = tempfile.mkdtemp(prefix="pero_")


def main():
    if not os.path.isdir(pero_temp_path):
        os.makedirs(pero_temp_path, mode=0o777)
    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--image_path", help="Input image path.", required=True)
    parser.add_argument("-oO", "--output_txt", help="Output path for txt. ", required=True)
    parser.add_argument("-oA", "--output_alto", help="Output path for alto.", required=True)
    parser.add_argument("-key", "--api_key", help="Api Key for your Organization", required=True)
    args = parser.parse_args()

    if os.path.exists(args.output_txt) and os.path.exists(args.output_alto):
        exit(0)

    is_converted = False
    image_path = args.image_path
    file_extension = os.path.basename(args.image_path).split(".")[1]
    if file_extension == "tif" or file_extension == "tiff":
        if os.path.exists(os.path.join(os.path.dirname(args.image_path), os.path.basename(args.image_path).split(".")[0] + ".jpg")):
            image_path = os.path.join(os.path.dirname(args.image_path), os.path.basename(args.image_path).split(".")[0] + ".jpg")
            file_name = os.path.basename(args.image_path).split(".")[0]
            is_converted = False
        else:
            image_path = convert_tif(args.image_path)
            file_name = os.path.basename(image_path).split(".")[0]
            is_converted = True
    else:
        file_name = os.path.basename(args.image_path).split(".")[0]
    content_type = get_content_type(file_extension)

    data = create_json(file_name)
    txt_format = "txt"
    alto_format = "alto"

    session = requests.Session()
    request_id = post_processing_request(session, data, args.api_key)

    try:
        upload_image(session, request_id, file_name, image_path, content_type, args.api_key)
    except FileNotFoundError as err:
        sys.stderr.write(f"Error: {err}")
        exit(-1)
    except OSError as err:
        sys.stderr.write(f"Error: {err}")
        exit(-1)
    except Exception as err:
        sys.stderr.write(f"Error: {err}")
        exit(-1)

    processing_result = ""
    while processing_result != "PROCESSED":
        processing_result = download_results(session, args.output_txt, request_id, file_name, txt_format, args.api_key)
        if processing_result == "PROCESSED":
            download_results(session, args.output_alto, request_id, file_name, alto_format, args.api_key)
        else:
            time.sleep(5)

    if is_converted:
        os.remove(image_path)
    if os.path.isdir(pero_temp_path):
        os.rmdir(pero_temp_path)
    #sys.stdout.write("Script finished successfully.")
    exit(0)


def create_json(file_name):
    output_data = {file_name: None}
    data_dict = {"engine": 1, "images": output_data}
    data = json.dumps(data_dict, ensure_ascii=False)
    return data


def post_processing_request(session, data, apiKey):
    url = posixpath.join(SERVER_URL, "post_processing_request")
    json_header={"api-key": apiKey, "Content-Type": "application/json"}
    response = session.post(url, data=data, headers=json_header)
    if response.status_code < 400:
        response_dict = response.json()
        while response_dict.get('status') != "success":
            time.sleep(15)
        request_id = response_dict.get('request_id')
        return request_id
    else:
        sys.stderr.write(f"The post processing request ended with status code {response.status_code}.")
        exit(-1)


def upload_image(session, request_id, file_name, image_path, content_type, apiKey):
    #url = os.path.join(SERVER_URL, "upload_image", request_id, file_name)
    url = posixpath.join(SERVER_URL, "upload_image", request_id, file_name)
    m = MultipartEncoder(fields={'file': (image_path, open(image_path, 'rb'), content_type)})
    json_header={"api-key": apiKey, "Content-Type": m.content_type}
    response = session.post(url, data=m, headers=json_header)
    if response.status_code >= 400:
        sys.stderr.write(f"The image upload of {file_name} ended with status code {response.status_code}.")
        exit(-1)
    else:
        pass


def download_results(session, output_dir, request_id, file_name, result_format, apiKey):
    url = posixpath.join(SERVER_URL, "download_results", request_id, file_name, result_format)
    json_header={"api-key": apiKey, "Content-Type": "application/json"}
    response = session.get(url, headers=json_header)
    if response.status_code == 200:
        with open(output_dir, "w", encoding="utf-8") as f:
            f.write(response.text)
            f.flush()
        return "PROCESSED"
    elif response.status_code >= 400:
        msg: str = response.json()["message"]
        sys.stderr.write(f"The request returned status code {response.status_code}."
                         f"The message is: {msg}")
        if "not processed yet" in response.json()["message"]:
            return "UNPROCESSED"
        exit(-1)
    else:
        return "UNPROCESSED"


def get_content_type(file_extension):
    tiff = ["tiff", "tif"]
    jpg = ["jpg", "jpeg", "JPG"]
    jp2 = ["jp2"]
    if file_extension in tiff:
        return "image/tiff"
    elif file_extension in jpg:
        return "image/jpeg"
    elif file_extension in jp2:
        return "image/jp2"
    else:
        sys.stderr.write(f"Error: the extension {file_extension} is not supported.")
        exit(-1)


def convert_tif(image_path):
    if os.path.basename(image_path).split(".")[1] == "tiff":
        output_file = os.path.basename(image_path).replace(".tiff", ".jpg")
    else:
        output_file = os.path.basename(image_path).replace(".tif", ".jpg")
    with Image.open(image_path) as img:
        try:
            output_path = os.path.join(pero_temp_path, output_file)
            img.save(output_path, "JPEG", quality=50)
        except OSError as err:
            sys.stderr.write(f"Error: {err}")
            exit(-1)
        except Exception as err:
            sys.stderr.write(f"Error: {err}")
            exit(-1)
    return output_path


if __name__ == "__main__":
    main()
